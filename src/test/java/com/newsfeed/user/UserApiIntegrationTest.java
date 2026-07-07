package com.newsfeed.user;

import com.newsfeed.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * user 컨텍스트 E2E — 실제 MySQL/Redis(Testcontainers)를 거쳐
 * 웹 어댑터 → 유스케이스 → 영속성/캐시 어댑터 전체를 검증한다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class UserApiIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    StringRedisTemplate redisTemplate;

    @Test
    void 사용자_생성부터_팔로우_언팔로우까지_전체_시나리오() throws Exception {
        long alice = createUser("alice", "Alice Kim");
        long bob = createUser("bob", "Bob Lee");

        // 조회로 횟수 캐시를 예열한다 (cnt:user:{alice} 적재)
        mockMvc.perform(get("/api/users/{id}", alice).header("X-USER-ID", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("alice"))
                .andExpect(jsonPath("$.followerCount").value(0));

        // bob이 alice를 팔로우
        mockMvc.perform(post("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());

        // 커밋 후 캐시 동기화(HINCRBY)가 반영되어 팔로워 수가 1이 된다
        mockMvc.perform(get("/api/users/{id}", alice).header("X-USER-ID", bob))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.followerCount").value(1));

        // Redis 횟수 캐시에 직접 반영됐는지 확인 (DB 재조회가 아니라 증분 갱신)
        assertThat(redisTemplate.opsForHash().get("cnt:user:" + alice, "followers")).isEqualTo("1");

        // 중복 팔로우는 409
        mockMvc.perform(post("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_FOLLOWING"));

        // 언팔로우 후 횟수 원복, 언팔로우는 멱등이라 반복해도 204
        mockMvc.perform(delete("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete("/api/users/{id}/follow", alice).header("X-USER-ID", bob))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/users/{id}", alice).header("X-USER-ID", bob))
                .andExpect(jsonPath("$.followerCount").value(0));
    }

    @Test
    void 중복_username은_409다() throws Exception {
        createUser("charlie", "Charlie");
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"charlie\",\"displayName\":\"다른 찰리\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DUPLICATE_USERNAME"));
    }

    @Test
    void 자기_자신_팔로우는_400이다() throws Exception {
        long dave = createUser("dave", "Dave");
        mockMvc.perform(post("/api/users/{id}/follow", dave).header("X-USER-ID", dave))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("CANNOT_FOLLOW_SELF"));
    }

    @Test
    void 인증_헤더가_없으면_401이다() throws Exception {
        mockMvc.perform(get("/api/users/1"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void 존재하지_않는_사용자_조회는_404다() throws Exception {
        mockMvc.perform(get("/api/users/999999").header("X-USER-ID", 1))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"));
    }

    @Test
    void 유효성_검증_실패는_400이다() throws Exception {
        mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"\",\"displayName\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private long createUser(String username, String displayName) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/users")
                        .contentType("application/json")
                        .content("{\"username\":\"%s\",\"displayName\":\"%s\"}"
                                .formatted(username, displayName)))
                .andExpect(status().isCreated())
                .andReturn();
        String body = result.getResponse().getContentAsString();
        return Long.parseLong(body.replaceAll(".*\"id\":(\\d+).*", "$1"));
    }
}
