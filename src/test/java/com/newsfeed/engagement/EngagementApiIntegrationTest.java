package com.newsfeed.engagement;

import com.newsfeed.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 6단계 end-to-end: 좋아요/취소/답글이 실제로 posts.like_count·reply_count(DB)와
 * cnt:post 캐시(Redis)에 반영되고, 그 결과가 GET /api/feed의 likeCount/replyCount/likedByMe로
 * 이어지는지 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class EngagementApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 좋아요와_답글이_피드의_횟수와_likedByMe에_반영된다() throws Exception {
        long alice = createUser("alice-engage", "Alice Engage");
        long bob = createUser("bob-engage", "Bob Engage");

        mockMvc.perform(post("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        long postId = createPost(alice, "engagement 테스트 포스트");
        awaitFeedContains(bob, postId); // 팬아웃이 먼저 반영돼야 이후 단언이 의미 있다

        // 좋아요 — 멱등: 두 번 눌러도 204, 횟수는 1로 고정
        mockMvc.perform(post("/api/posts/{id}/like", postId).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/posts/{id}/like", postId).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        // 답글 작성
        MvcResult replyResult = mockMvc.perform(post("/api/posts/{id}/replies", postId)
                        .header("X-USER-ID", bob)
                        .contentType("application/json")
                        .content("{\"content\":\"좋은 글이네요\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        assertThat(replyResult.getResponse().getContentAsString()).contains("\"postId\":" + postId);

        String feedBody = awaitFeedShowsCounts(bob, postId, 1, 1);
        assertThat(feedBody).contains("\"likedByMe\":true");

        // 답글 목록 조회
        mockMvc.perform(get("/api/posts/{id}/replies", postId).header("X-USER-ID", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].content").value("좋은 글이네요"));

        // 좋아요 취소 — 멱등: 두 번 취소해도 204, 횟수는 0으로
        mockMvc.perform(delete("/api/posts/{id}/like", postId).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/posts/{id}/like", postId).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        String afterUnlike = awaitFeedShowsCounts(bob, postId, 0, 1);
        assertThat(afterUnlike).contains("\"likedByMe\":false");
    }

    @Test
    void 존재하지_않는_포스트에_좋아요하면_404다() throws Exception {
        long bob = createUser("bob-engage-404", "Bob 404");
        mockMvc.perform(post("/api/posts/{id}/like", 999999).header("X-USER-ID", bob))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
    }

    private String awaitFeedContains(long userId, long postId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            String body = fetchFeed(userId);
            if (body.contains("\"postId\":" + postId)) {
                return body;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("피드에 postId=" + postId + "가 나타나지 않았습니다: " + fetchFeed(userId));
    }

    private String awaitFeedShowsCounts(long userId, long postId, int likeCount, int replyCount) throws Exception {
        String needle = "\"postId\":" + postId + ",\"content\":\"engagement 테스트 포스트\"";
        String countsNeedle = "\"likeCount\":" + likeCount + ",\"replyCount\":" + replyCount;
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        String lastBody = "";
        while (Instant.now().isBefore(deadline)) {
            lastBody = fetchFeed(userId);
            if (lastBody.contains(countsNeedle)) {
                return lastBody;
            }
            Thread.sleep(200);
        }
        throw new AssertionError("피드가 likeCount=" + likeCount + ", replyCount=" + replyCount
                + "를 보여주지 않았습니다: " + lastBody + " (needle=" + needle + ")");
    }

    private String fetchFeed(long userId) throws Exception {
        return mockMvc.perform(get("/api/feed").header("X-USER-ID", userId))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }

    private long createUser(String username, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"%s\",\"displayName\":\"%s\"}".formatted(username, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private long createPost(long authorId, String content) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/posts")
                        .header("X-USER-ID", authorId)
                        .contentType("application/json")
                        .content("{\"content\":\"%s\"}".formatted(content)))
                .andExpect(status().isCreated())
                .andReturn();
        return extractId(result);
    }

    private long extractId(MvcResult result) throws Exception {
        String body = result.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
