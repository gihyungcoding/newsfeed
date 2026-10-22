package com.newsfeed.post;

import com.newsfeed.TestcontainersConfiguration;
import com.newsfeed.common.cache.RedisKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

import static com.newsfeed.support.ApiFixtures.createPost;
import static com.newsfeed.support.ApiFixtures.createUser;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 4단계 핵심 파이프라인의 end-to-end 검증: 포스트 발행 → post-created Kafka 이벤트 →
 * 팬아웃 워커 소비 → 팔로워의 feed:{followerId} Redis 캐시에 postId가 나타나는지 확인한다.
 *
 * <p>Kafka 소비는 별도 스레드에서 비동기로 일어나므로, 응답이 200이어도 팬아웃이 아직
 * 끝나지 않았을 수 있다 — 그래서 결과를 즉시 단언하지 않고 폴링한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class PostFanoutIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void 포스트_발행이_팔로워의_피드_캐시에_팬아웃된다() throws Exception {
        long alice = createUser(mockMvc, "alice-fanout", "Alice");
        long bob = createUser(mockMvc, "bob-fanout", "Bob");

        mockMvc.perform(post("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        long postId = createPost(mockMvc, alice, "첫 팬아웃 테스트 포스트");

        Set<String> feedMembers = awaitFeedContains(bob, postId);

        assertThat(feedMembers).contains(String.valueOf(postId));
    }

    private Set<String> awaitFeedContains(long followerId, long postId) throws InterruptedException {
        String key = RedisKeys.feed(followerId);
        Instant deadline = Instant.now().plus(Duration.ofSeconds(10));
        while (Instant.now().isBefore(deadline)) {
            Set<String> members = redisTemplate.opsForZSet().range(key, 0, -1);
            if (members != null && members.contains(String.valueOf(postId))) {
                return members;
            }
            Thread.sleep(200);
        }
        return redisTemplate.opsForZSet().range(key, 0, -1);
    }
}
