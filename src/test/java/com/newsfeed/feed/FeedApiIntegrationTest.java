package com.newsfeed.feed;

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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 5단계 push 경로 end-to-end: 팔로우 → 포스트 발행 → (비동기) 팬아웃 → GET /api/feed 조회.
 * 팬아웃이 Kafka 컨슈머 스레드에서 비동기로 처리되므로, 피드에 반영될 때까지 폴링한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class FeedApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void 팔로우한_사람의_포스트가_피드에_나타난다() throws Exception {
        long alice = createUser("alice-feed", "Alice Feed");
        long bob = createUser("bob-feed", "Bob Feed");

        mockMvc.perform(post("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        long postId = createPost(alice, "피드 테스트 포스트");

        String body = awaitFeedContains(bob, postId);

        org.assertj.core.api.Assertions.assertThat(body).contains("\"postId\":" + postId);
        org.assertj.core.api.Assertions.assertThat(body).contains("\"username\":\"alice-feed\"");
    }

    @Test
    void 커서로_다음_페이지를_조회하면_이전_페이지와_겹치지_않는다() throws Exception {
        long alice = createUser("alice-cursor", "Alice Cursor");
        long bob = createUser("bob-cursor", "Bob Cursor");

        mockMvc.perform(post("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        long firstPostId = createPost(alice, "첫 포스트");
        Thread.sleep(10); // score(작성시각 millis)가 서로 달라야 순서를 구분할 수 있다
        long secondPostId = createPost(alice, "두번째 포스트");

        awaitFeedContains(bob, secondPostId);

        MvcResult page1Result = mockMvc.perform(get("/api/feed").param("size", "1").header("X-USER-ID", bob))
                .andExpect(status().isOk())
                .andReturn();
        String page1 = page1Result.getResponse().getContentAsString();
        org.assertj.core.api.Assertions.assertThat(page1).contains("\"postId\":" + secondPostId);

        long cursor = extractNextCursor(page1);

        MvcResult page2Result = mockMvc.perform(get("/api/feed")
                        .param("size", "1").param("cursor", String.valueOf(cursor))
                        .header("X-USER-ID", bob))
                .andExpect(status().isOk())
                .andReturn();
        String page2 = page2Result.getResponse().getContentAsString();

        // 회귀 방지: 커서를 넘겨도 1페이지의 postId가 재조회되는 버그가 있었다
        // (double 정밀도 문제로 cursor exclusive 처리가 무력화됨 — feed:{id} score가
        // epoch millis처럼 13자리 큰 수일 때 1e-6 같은 작은 epsilon은 반올림으로 사라진다)
        org.assertj.core.api.Assertions.assertThat(page2).contains("\"postId\":" + firstPostId);
        org.assertj.core.api.Assertions.assertThat(page2).doesNotContain("\"postId\":" + secondPostId);
    }

    private long extractNextCursor(String body) {
        return Long.parseLong(body.replaceAll(".*\"nextCursor\":(\\d+).*", "$1"));
    }

    private String awaitFeedContains(long userId, long postId) throws Exception {
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            MvcResult result = mockMvc.perform(get("/api/feed").header("X-USER-ID", userId))
                    .andExpect(status().isOk())
                    .andReturn();
            String body = result.getResponse().getContentAsString();
            if (body.contains("\"postId\":" + postId)) {
                return body;
            }
            Thread.sleep(200);
        }
        return mockMvc.perform(get("/api/feed").header("X-USER-ID", userId))
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
