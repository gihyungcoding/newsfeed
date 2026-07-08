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

    // 도메인 Post에는 없다(§콘텐츠·횟수 캐시 분리) — @Modifying 증분 쿼리가 타깃으로 삼는
    // 컬럼일 뿐이라 여기 raw 필드로만 존재한다. findCounts()가 이 값을 읽어 PostCounts로 변환한다.
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
        entity.createdAt = post.createdAt();
        return entity;
    }

    Post toDomain() {
        return new Post(id, authorId, content, createdAt);
    }

    Long getId() {
        return id;
    }

    int getLikeCount() {
        return likeCount;
    }

    int getReplyCount() {
        return replyCount;
    }
}
