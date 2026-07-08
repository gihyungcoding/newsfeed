package com.newsfeed.engagement.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.springframework.data.domain.Persistable;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

/** user 컨텍스트의 FollowJpaEntity와 완전히 같은 패턴 — 복합 PK + Persistable로 merge 대신 항상 insert. */
@Entity
@Table(name = "post_likes")
public class LikeJpaEntity implements Persistable<LikeJpaEntity.LikeId> {

    @EmbeddedId
    private LikeId id;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LikeJpaEntity() {
    }

    LikeJpaEntity(long postId, long userId) {
        this.id = new LikeId(postId, userId);
        this.createdAt = Instant.now();
    }

    @Override
    public LikeId getId() {
        return id;
    }

    @Override
    public boolean isNew() {
        return true;
    }

    @Embeddable
    public static class LikeId implements Serializable {

        @Column(name = "post_id")
        private Long postId;

        @Column(name = "user_id")
        private Long userId;

        protected LikeId() {
        }

        LikeId(long postId, long userId) {
            this.postId = postId;
            this.userId = userId;
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof LikeId other
                    && Objects.equals(postId, other.postId)
                    && Objects.equals(userId, other.userId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(postId, userId);
        }
    }
}
