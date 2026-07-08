package com.newsfeed.engagement.adapter.out.persistence;

import com.newsfeed.engagement.domain.Reply;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "replies")
public class ReplyJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 300)
    private String content;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReplyJpaEntity() {
    }

    static ReplyJpaEntity from(Reply reply) {
        ReplyJpaEntity entity = new ReplyJpaEntity();
        entity.postId = reply.postId();
        entity.authorId = reply.authorId();
        entity.content = reply.content();
        entity.createdAt = reply.createdAt();
        return entity;
    }

    Reply toDomain() {
        return new Reply(id, postId, authorId, content, createdAt);
    }
}
