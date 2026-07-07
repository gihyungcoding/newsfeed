package com.newsfeed.feed;

import com.newsfeed.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * celebrity-threshold=0으로 "모든 작성자가 celebrity"인 상황을 만든다. 팬아웃(push)은
 * 생략되지만, GET /api/feed는 author-posts 캐시를 pull해 여전히 포스트를 보여줘야 한다 —
 * 이게 하이브리드 팬아웃의 핵심(§3.1, §3.4)이고, PostFanoutCelebrityIntegrationTest(4단계)가
 * "push되지 않음"을 확인했다면 이 테스트는 "그래도 조회는 된다"를 확인한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "newsfeed.fanout.celebrity-threshold=0")
class FeedApiCelebrityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    void celebrity의_포스트는_팬아웃_없이도_pull로_피드에_나타난다() throws Exception {
        long celebrity = createUser("celeb-feed", "Celebrity Feed");
        long fan = createUser("fan-feed", "Fan Feed");

        mockMvc.perform(post("/api/users/{id}/follow", celebrity).header("X-USER-ID", fan))
                .andExpect(status().isNoContent());

        long postId = createPost(celebrity, "celebrity pull 테스트 포스트");

        // Kafka 팬아웃이 비동기라도, celebrity는 애초에 팬아웃 대상이 아니므로 대기할 필요가 없다.
        // author-posts 캐시 적재는 PostCacheSynchronizer가 같은 요청 스레드에서 동기로 처리한다.
        MvcResult result = mockMvc.perform(get("/api/feed").header("X-USER-ID", fan))
                .andExpect(status().isOk())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).contains("\"postId\":" + postId);
        assertThat(body).contains("\"username\":\"celeb-feed\"");
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
