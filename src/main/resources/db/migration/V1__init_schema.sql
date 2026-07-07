-- 뉴스피드 시스템 초기 스키마
-- 횟수(like_count, reply_count, follower_count, following_count)는 비정규화 컬럼:
-- 원천은 각 행(post_likes, replies, follows)이며, 쓰기 트랜잭션에서 원자적으로 증감한다.
-- (docs/03-detailed-design.md §3.2 횟수 캐시의 정합성 전략)

CREATE TABLE users (
    id              BIGINT       NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)  NOT NULL,
    display_name    VARCHAR(100) NOT NULL,
    follower_count  INT          NOT NULL DEFAULT 0,
    following_count INT          NOT NULL DEFAULT 0,
    created_at      TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_username (username)
);

CREATE TABLE follows (
    follower_id BIGINT       NOT NULL,
    followee_id BIGINT       NOT NULL,
    created_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (follower_id, followee_id),
    -- 팬아웃 워커가 "followee의 팔로워 목록"을 조회하는 인덱스
    KEY idx_follows_followee (followee_id, follower_id),
    CONSTRAINT fk_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id),
    CONSTRAINT fk_follows_followee FOREIGN KEY (followee_id) REFERENCES users (id)
);

CREATE TABLE posts (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    author_id   BIGINT       NOT NULL,
    content     VARCHAR(500) NOT NULL,
    like_count  INT          NOT NULL DEFAULT 0,
    reply_count INT          NOT NULL DEFAULT 0,
    created_at  TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- 작성자별 최신 포스트 조회 (celebrity pull, 콜드 스타트 피드 재구성)
    KEY idx_posts_author_created (author_id, created_at DESC),
    CONSTRAINT fk_posts_author FOREIGN KEY (author_id) REFERENCES users (id)
);

CREATE TABLE post_likes (
    post_id    BIGINT       NOT NULL,
    user_id    BIGINT       NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    -- 복합 PK가 중복 좋아요를 차단한다 (멱등성의 근거)
    PRIMARY KEY (post_id, user_id),
    CONSTRAINT fk_post_likes_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_post_likes_user FOREIGN KEY (user_id) REFERENCES users (id)
);

CREATE TABLE replies (
    id         BIGINT       NOT NULL AUTO_INCREMENT,
    post_id    BIGINT       NOT NULL,
    author_id  BIGINT       NOT NULL,
    content    VARCHAR(300) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    PRIMARY KEY (id),
    -- 포스트별 최신 답글 조회
    KEY idx_replies_post_created (post_id, created_at DESC),
    CONSTRAINT fk_replies_post FOREIGN KEY (post_id) REFERENCES posts (id),
    CONSTRAINT fk_replies_author FOREIGN KEY (author_id) REFERENCES users (id)
);
