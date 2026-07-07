package com.newsfeed.user.adapter.out.persistence;

import com.newsfeed.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * JPA 매핑 전용 엔티티. 도메인 모델({@link User})과 분리되어 있어
 * 영속성 기술이 도메인에 스며들지 않는다 (매핑 비용을 지불하는 대신 얻는 격리).
 */
@Entity
@Table(name = "users")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String username;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Column(name = "follower_count", nullable = false)
    private int followerCount;

    @Column(name = "following_count", nullable = false)
    private int followingCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected UserJpaEntity() {
    }

    static UserJpaEntity from(User user) {
        UserJpaEntity entity = new UserJpaEntity();
        entity.username = user.username();
        entity.displayName = user.displayName();
        entity.createdAt = user.createdAt();
        return entity;
    }

    User toDomain() {
        return new User(id, username, displayName, createdAt);
    }

    Long getId() {
        return id;
    }

    int getFollowerCount() {
        return followerCount;
    }

    int getFollowingCount() {
        return followingCount;
    }
}
