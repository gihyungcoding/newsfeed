package com.newsfeed.post;

import com.newsfeed.TestcontainersConfiguration;
import com.newsfeed.common.cache.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * celebrity-threshold를 0으로 낮춰 "모든 작성자가 celebrity"인 상황을 만들고,
 * 팬아웃이 실제로 생략되는지(= 팔로워 피드 캐시에 postId가 나타나지 않는지) 검증한다.
 * 별도 프로퍼티가 필요해 별도 Spring 컨텍스트(및 별도 테스트 클래스)로 분리했다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@TestPropertySource(properties = "newsfeed.fanout.celebrity-threshold=0")
class PostFanoutCelebrityIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void celebrity_작성자의_포스트는_팔로워_피드에_팬아웃되지_않는다() throws Exception {
        long celebrity = createUser("celeb-fanout", "Celebrity");
        long follower = createUser("fan-fanout", "Fan");

        mockMvc.perform(post("/api/users/{id}/follow", celebrity).header("X-USER-ID", follower))
                .andExpect(status().isNoContent());

        long postId = createPost(celebrity, "celebrity 포스트");

        // 비동기 팬아웃이 (혹시라도) 처리될 시간을 넉넉히 준 뒤에도 비어 있어야 한다
        Thread.sleep(2000);
        Set<String> feedMembers = redisTemplate.opsForZSet().range(RedisKeys.feed(follower), 0, -1);

        assertThat(feedMembers == null || !feedMembers.contains(String.valueOf(postId))).isTrue();
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
