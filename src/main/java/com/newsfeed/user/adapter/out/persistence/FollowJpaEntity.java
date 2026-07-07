package com.newsfeed.user.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "follows")
public class FollowJpaEntity implements Persistable<FollowJpaEntity.FollowId> {

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

    @Override
    public FollowId getId() {
        return id;
    }

    /**
     * 복합키가 미리 채워져 있으면 save()가 merge(SELECT 후 UPDATE)로 동작한다.
     * 그러면 동시 중복 팔로우가 PK 위반 대신 조용한 UPDATE가 되어 횟수가 이중 증가한다.
     * 팔로우 관계는 항상 신규 INSERT이므로 isNew=true로 고정해 위반이 예외로 드러나게 한다.
     */
    @Override
    public boolean isNew() {
        return true;
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
