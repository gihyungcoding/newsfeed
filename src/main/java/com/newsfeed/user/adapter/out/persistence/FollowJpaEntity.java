package com.newsfeed.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "follows")
public class FollowJpaEntity {

    @EmbeddedId
    private FollowId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FollowJpaEntity() {
    }

    FollowJpaEntity(long followerId, long followeeId) {
        this.id = new FollowId(followerId, followeeId);
        this.createdAt = Instant.now();
    }

    /** (follower_id, followee_id) 복합 기본키 — 중복 팔로우를 DB 수준에서 차단한다. */
    @Embeddable
    public static class FollowId implements Serializable {

        @Column(name = "follower_id")
        private Long followerId;

        @Column(name = "followee_id")
        private Long followeeId;

        protected FollowId() {
        }

        FollowId(long followerId, long followeeId) {
            this.followerId = followerId;
            this.followeeId = followeeId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof FollowId other
                    && Objects.equals(followerId, other.followerId)
                    && Objects.equals(followeeId, other.followeeId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(followerId, followeeId);
        }
    }
}
