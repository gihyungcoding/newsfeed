# 4. API 명세

## 공통 사항

- Base URL: `http://localhost:8080`
- 인증: `X-USER-ID: {userId}` 헤더 (사용자 생성 API 제외 전부 필수)
- 처리율 제한 초과 시: `429 Too Many Requests`
- 오류 응답 형식:

```json
{
  "code": "USER_NOT_FOUND",
  "message": "사용자를 찾을 수 없습니다: 42"
}
```

## 4.1 사용자 (user 컨텍스트)

### 사용자 생성

```
POST /api/users
Content-Type: application/json

{ "username": "alice", "displayName": "Alice Kim" }
```

- 응답 `201 Created`

```json
{ "id": 1, "username": "alice", "displayName": "Alice Kim", "createdAt": "2026-07-06T12:00:00Z" }
```

- `409 Conflict`: username 중복

### 사용자 조회

```
GET /api/users/{userId}
```

- 응답 `200 OK` (위와 동일 형태 + `followerCount`, `followingCount`)
- `404 Not Found`

### 팔로우 / 언팔로우

```
POST   /api/users/{targetUserId}/follow     # 인증 사용자가 target을 팔로우
DELETE /api/users/{targetUserId}/follow
```

- 응답 `204 No Content`
- `400 Bad Request`: 자기 자신 팔로우
- `409 Conflict`: 이미 팔로우 중 (POST의 경우)

## 4.2 포스트 (post 컨텍스트)

### 포스트 발행

```
POST /api/posts
X-USER-ID: 1
Content-Type: application/json

{ "content": "첫 번째 포스트입니다." }
```

- 응답 `201 Created`

```json
{ "id": 100, "authorId": 1, "content": "첫 번째 포스트입니다.", "createdAt": "2026-07-06T12:34:56Z" }
```

- 유효성: content 1~500자

### 특정 사용자의 포스트 목록

```
GET /api/users/{userId}/posts?cursor={epochMillis}&size=20
```

- 응답 `200 OK`

```json
{
  "items": [ { "id": 100, "authorId": 1, "content": "...", "createdAt": "..." } ],
  "nextCursor": 1720262096000
}
```

## 4.3 좋아요 / 답글 (engagement 컨텍스트)

### 좋아요 / 좋아요 취소

```
POST   /api/posts/{postId}/like
DELETE /api/posts/{postId}/like
X-USER-ID: 2
```

- 응답 `204 No Content` (멱등 — 이미 좋아요한 포스트에 다시 POST해도 204)
- `404 Not Found`: 포스트 없음

### 답글 작성

```
POST /api/posts/{postId}/replies
X-USER-ID: 2
Content-Type: application/json

{ "content": "좋은 글이네요!" }
```

- 응답 `201 Created`

```json
{ "id": 500, "postId": 100, "authorId": 2, "content": "좋은 글이네요!", "createdAt": "2026-07-06T13:00:00Z" }
```

- 유효성: content 1~300자

### 답글 목록

```
GET /api/posts/{postId}/replies?cursor={epochMillis}&size=20
```

- 응답 `200 OK`: `{ "items": [...], "nextCursor": ... }` (작성 시각 역순)

## 4.4 뉴스피드 (feed 컨텍스트)

### 피드 조회

```
GET /api/feed?cursor={epochMillis}&size=20
X-USER-ID: 2
```

- 응답 `200 OK`

```json
{
  "items": [
    {
      "postId": 100,
      "content": "첫 번째 포스트입니다.",
      "createdAt": "2026-07-06T12:34:56Z",
      "author": { "id": 1, "username": "alice", "displayName": "Alice Kim" },
      "likeCount": 3,
      "replyCount": 1,
      "likedByMe": true
    }
  ],
  "nextCursor": 1720262096000
}
```

- `cursor` 생략 시 최신부터, `nextCursor`가 `null`이면 마지막 페이지
- 내부 동작: 피드 캐시(push분) + celebrity 포스트(pull분) 병합, 횟수·likedByMe는 행동/횟수 캐시에서 조립 — [03-detailed-design.md](03-detailed-design.md) §3.4

## 4.5 내부 이벤트 (Kafka)

### `post-created` 토픽

```json
{ "postId": 100, "authorId": 1, "createdAt": 1720262096000 }
```

- 파티션 키: `authorId` (작성자별 순서 보장)
- 컨슈머: 팬아웃 워커 (fanout 컨텍스트)
