package com.newsfeed.post.adapter.out.persistence;

import com.newsfeed.post.domain.Post;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "posts")
public class PostJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(nullable = false, length = 500)
    private String content;

    @Column(name = "like_count", nullable = false)
    private int likeCount;

    @Column(name = "reply_count", nullable = false)
    private int replyCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PostJpaEntity() {
    }

    static PostJpaEntity from(Post post) {
        PostJpaEntity entity = new PostJpaEntity();
        entity.authorId = post.authorId();
        entity.content = post.content();
        entity.likeCount = post.likeCount();
        entity.replyCount = post.replyCount();
        entity.createdAt = post.createdAt();
        return entity;
    }

    Post toDomain() {
        return new Post(id, authorId, content, likeCount, replyCount, createdAt);
    }
}
