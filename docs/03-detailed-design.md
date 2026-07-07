# 3. 상세 설계 (Detailed Design)

## 3.1 팬아웃 전략: 쓰기 vs 읽기 vs 하이브리드

### 팬아웃 쓰기 (Fanout-on-Write, Push 모델)

포스트 발행 시점에 모든 팔로워의 피드 캐시에 미리 밀어 넣는다.

| 장점 | 단점 |
|------|------|
| 피드 조회가 빠르다 (미리 계산됨) | 팔로워가 많으면 발행 비용 폭증 (핫키 문제) |
| 조회 시점 연산이 거의 없다 | 비활성 사용자에게도 쓰기 낭비 |

### 팬아웃 읽기 (Fanout-on-Read, Pull 모델)

조회 시점에 팔로우한 사람들의 최근 포스트를 모아서 조립한다.

| 장점 | 단점 |
|------|------|
| 비활성 사용자에게 자원 낭비 없음 | 조회가 느리다 (매번 N명의 포스트 취합) |
| 핫키 문제 없음 | read-heavy 시스템에서 조회 부하 급증 |

### 채택: 하이브리드

- **일반 사용자의 포스트** → 팬아웃 쓰기 (대부분의 조회를 빠르게)
- **celebrity의 포스트** → 팬아웃 생략, 조회 시 pull하여 병합

**celebrity 판정**: 팔로워 수 > 임계값(`newsfeed.fanout.celebrity-threshold`, 기본 1,000 — 로컬 테스트에서 조정 가능한 설정값).
판정 시점은 **이벤트 소비 시점의 팔로워 수** 기준으로 한다. 판정이 바뀌는 경계 상황(팔로워 수가 임계값을 넘나드는 경우)에서 일부 포스트가 push/pull 양쪽에 걸칠 수 있으므로, 조회 시 병합 단계에서 **postId 기준 중복 제거**를 한다.

## 3.2 캐시 설계 — 책의 5계층 구조

책의 뉴스피드 캐시 5계층(뉴스피드, 콘텐츠, 소셜 그래프, 행동, 횟수)을 전부 구현한다.

| 계층 | 대상 | Redis 키 | 자료구조 | 정책 |
|------|------|----------|----------|------|
| **뉴스피드** | 뉴스 피드 | `feed:{userId}` | Sorted Set (member=postId, score=작성시각 epoch millis) | 상한 500개 (`ZREMRANGEBYRANK feed:{id} 0 -501`), TTL 30일 (비활성 유저 자연 소멸) |
| **콘텐츠** | 일반 콘텐츠 | `post:{postId}` | String (JSON) | look-aside, TTL 1시간 |
| | 인기 콘텐츠 | `author-posts:{celebrityId}` | Sorted Set (member=postId, score=작성시각) | celebrity의 최근 포스트 목록. 조회 시 pull 경로가 매번 DB를 치지 않도록 캐시. 발행 시 write-through, TTL 1시간 |
| | 작성자 프로필 | `user:{userId}` | Hash `{username, displayName, createdAt}` | look-aside, TTL 1시간. 피드 아이템의 작성자 정보 조립에 사용 |
| **소셜 그래프** | 팔로잉 | `following:{userId}` | Set (userId) | look-aside, TTL 1시간. 피드 조회 시 celebrity 필터링에 사용 |
| | 팔로어 | (캐시 안 함 — DB 배치 조회) | — | 팬아웃 워커는 이벤트당 1회, 페이지 단위로 DB에서 직접 읽는다. 반복 조회가 아니라 캐시 실익이 낮음 |
| **행동** | 좋아요 | `post-likers:{postId}` | Set (userId) | look-aside, TTL 1시간. "내가 좋아요 했는지(likedByMe)" 판정 |
| | 답글 | `post-replies:{postId}` | Sorted Set (member=replyId, score=작성시각) | 최근 답글 ID 목록, look-aside, TTL 1시간 |
| **횟수** | 좋아요·답글 횟수 | `cnt:post:{postId}` | Hash `{likes, replies}` | look-aside + 쓰기 시 `HINCRBY` 증분, TTL 1시간 |
| | 팔로어·팔로잉 횟수 | `cnt:user:{userId}` | Hash `{followers, following}` | look-aside + `HINCRBY` 증분. **celebrity 판정에 사용** (매번 `COUNT` 쿼리 방지) |

### look-aside(cache-aside) 패턴

```
읽기: 캐시 조회 → miss면 DB 조회 → 캐시 적재 → 반환
쓰기: DB 갱신 → 캐시 무효화(또는 증분 갱신)
```

- 포스트·답글은 불변(수정 기능 없음)으로 설계해 캐시 정합성 문제를 단순화한다.
- 피드 캐시가 통째로 없는 경우(콜드 스타트, TTL 만료)는 DB에서 팔로잉들의 최근 포스트를 조회해 피드를 재구성하고 캐시를 다시 채운다.

### 횟수 캐시의 정합성 전략

횟수의 **원천(source of truth)은 DB 행**(`post_likes`, `replies`, `follows`의 실제 행 수)이다. 조회 성능을 위해 두 겹으로 비정규화한다.

1. DB `posts.like_count`, `posts.reply_count`, `users.follower_count` 컬럼 — 좋아요/답글/팔로우 트랜잭션 안에서 원자적 `UPDATE ... SET x = x + 1`
2. Redis `cnt:*` Hash — 같은 쓰기 경로에서 `HINCRBY` (키가 있을 때만), miss 시 DB 컬럼값으로 재적재

Redis 증분이 유실돼도(TTL 만료, 장애) 다음 miss 때 DB 컬럼값으로 복구되므로 오차가 자가 치유된다.

### 키 관리 실무 규칙

- **키 카탈로그 중앙화**: 위 표의 모든 키 패턴은 `com.newsfeed.common.cache.RedisKeys`에서만 조립한다. 어댑터마다 문자열을 하드코딩하면 오타 버그와 "이 키를 누가 쓰는지" 추적 불가 문제가 생긴다.
- **TTL 지터**: 동시에 대량 적재된 키들이 같은 시각에 만료되면 그 순간 DB 조회가 몰리는 "캐시 눈사태"가 생긴다. `com.newsfeed.common.cache.JitteredTtl`로 TTL에 ±10% 무작위 편차를 더해 만료 시점을 흩뿌린다.
- **빅키 방지**: `feed:{userId}`처럼 무한히 자랄 수 있는 컬렉션은 반드시 상한을 강제한다 (`ZREMRANGEBYRANK`로 500개 컷). 상한 없는 컬렉션형 키는 만들지 않는다.
- **TTL 없는 키 금지**: 예외적으로 영구 보관이 필요한 경우가 아니면 모든 키에 TTL을 둔다. 마지막 방어선으로 Redis `maxmemory` + eviction 정책(`allkeys-lru`)을 설정한다 (인프라 설정, 이 저장소 범위 밖).

## 3.3 팬아웃 워커 상세

```
1. Kafka post-created 이벤트 수신 {postId, authorId, createdAt}
2. authorId의 팔로워 수 조회
3. 팔로워 수 > celebrity 임계값 → 종료 (팬아웃 생략)
4. 팔로워 ID를 페이지 단위(예: 1,000명)로 배치 조회
5. 배치마다 Redis pipeline으로:
   - ZADD feed:{followerId} {createdAt} {postId}
   - ZREMRANGEBYRANK feed:{followerId} 0 -501   (상한 유지)
   - EXPIRE feed:{followerId} 30d
```

**설계 결정**
- **페이지 단위 배치 조회**: 팔로워 5,000명을 한 번에 메모리에 올리지 않는다. 실제 대규모라면 팔로워 범위별로 메시지를 쪼개 여러 워커에 분산한다(로드맵에 기록).
- **Redis pipeline**: 팔로워 1,000명 = 3,000개 명령을 왕복 1번으로 처리.
- **멱등성**: `ZADD`는 같은 (postId) 재추가 시 score만 갱신되므로, Kafka 재전달(at-least-once)이 발생해도 피드가 중복되지 않는다.
- **컨슈머 그룹**: 파티션 키를 authorId로 잡아 동일 작성자의 이벤트 순서를 보장하면서 워커를 수평 확장할 수 있다.

## 3.4 피드 조회 상세 (하이브리드 read path)

```
GET /api/feed?cursor={epochMillis}&size=20

1. ZREVRANGEBYSCORE feed:{me} (cursor 미만) LIMIT size → 푸시된 postId 목록
2. 내가 팔로우한 celebrity 목록 조회 (follows ⋈ 팔로워 수 기준)
3. 각 celebrity의 최근 포스트를 pull (작성자별 포스트 캐시/DB)
4. 1 + 3을 createdAt 역순으로 병합, postId 중복 제거, size개 절단
5. postId → post 캐시(miss 시 DB) 조립, authorId → user 캐시 조립
6. postId → 횟수 캐시(`cnt:post:{id}`)에서 likeCount/replyCount, 행동 캐시(`post-likers:{id}`)에서 likedByMe 조립
7. 응답: items[] + nextCursor(마지막 아이템의 createdAt)
```

- **커서 기반 페이지네이션**: offset 방식은 새 포스트 유입 시 중복/누락이 생기므로 시각 기반 커서를 쓴다.
- 2단계의 celebrity 목록은 자주 바뀌지 않으므로 추후 캐시 대상(로드맵).

## 3.5 좋아요·답글 쓰기 경로 (engagement 컨텍스트)

```
좋아요:   POST /api/posts/{postId}/like
1. post_likes에 INSERT (PK = (post_id, user_id) → 중복 좋아요는 DB가 차단, 멱등)
2. 같은 트랜잭션에서 posts.like_count + 1
3. 커밋 후 Redis: SADD post-likers:{postId} {userId}, HINCRBY cnt:post:{postId} likes 1

좋아요 취소: DELETE — 위의 역연산 (DELETE 행, count - 1, SREM, HINCRBY -1)

답글:     POST /api/posts/{postId}/replies
1. replies에 INSERT + posts.reply_count + 1 (한 트랜잭션)
2. 커밋 후 Redis: ZADD post-replies:{postId}, HINCRBY cnt:post:{postId} replies 1, 답글 캐시 적재
```

- 좋아요/답글은 **팬아웃하지 않는다.** 피드 캐시에는 postId만 있고, 횟수·likedByMe는 조회 시점에 행동/횟수 캐시에서 조립하므로 포스트를 받은 5,000명의 피드를 갱신할 필요가 없다.
- Redis 갱신은 커밋 후(after-commit)에 수행한다. 롤백된 트랜잭션이 캐시를 오염시키지 않게 하기 위함이며, 캐시 갱신 실패는 무시해도 §3.2의 자가 치유(look-aside 재적재)로 복구된다.

## 3.6 웹 서버 계층: 인증과 처리율 제한

책의 웹 서버 역할(인증, rate limiting)을 단순화해 구현한다.

- **인증**: `X-USER-ID` 헤더를 신뢰하는 방식. 실제 서비스라면 JWT/세션이지만, 이 프로젝트의 학습 주제(피드 아키텍처)가 아니므로 인터셉터에서 헤더 → 사용자 검증만 수행한다. 인증 로직이 인터셉터 한 곳에 격리되어 있어 추후 JWT로 교체가 쉽다.
- **처리율 제한**: Redis `INCR` + `EXPIRE` 기반 고정 윈도 카운터 (`ratelimit:{userId}:{window}`). 사용자당 초당 N회 제한. (책 4장의 토큰 버킷 등 정교한 알고리즘은 로드맵.)

## 3.7 주요 트레이드오프 기록

| 결정 | 대안 | 선택 이유 |
|------|------|-----------|
| 팔로우 관계를 MySQL 테이블로 | 그래프 DB (책의 제안) | 필요한 질의가 1-depth(팔로워/팔로잉 목록)뿐. 친구 추천 같은 다중 hop 탐색이 생기면 그래프 DB 도입 검토 |
| 단일 애플리케이션 + 바운디드 컨텍스트 | 처음부터 마이크로서비스 | 학습 단계에서 운영 복잡도 최소화. 컨텍스트 간 통신을 포트/이벤트로 강제해 추출 비용을 낮춰 둠 ([05-msa-roadmap.md](05-msa-roadmap.md)) |
| Kafka | RabbitMQ, Redis Streams | 파티션 기반 순서 보장 + 컨슈머 그룹 확장 모델이 팬아웃 워커 구조와 잘 맞음. 실무 채택률도 높아 포트폴리오 가치 |
| 피드 캐시에 postId만 저장 | 포스트 전체 저장 | 메모리 절약, 포스트 데이터 단일 원천 유지 |
| 최종적 일관성 허용 | 동기 팬아웃 | 발행 지연을 팔로워 수와 무관하게 유지. 피드 노출의 초 단위 지연은 제품 특성상 허용 |
| 횟수를 DB 컬럼 + Redis 이중 비정규화 | 매번 `COUNT(*)` | read-heavy 시스템에서 COUNT는 비싸다. 원천은 행(row)으로 유지하고 컬럼·캐시는 파생값으로 취급해 오차 자가 치유 |
| 좋아요/답글은 팬아웃 없음 | 횟수 변경 시 피드 캐시 갱신 | 피드에는 postId만 있으므로 조회 시 조립으로 충분. 갱신 폭증(포스트당 팔로워 수 × 좋아요 수) 방지 |

## 3.8 클린 아키텍처 적용

각 바운디드 컨텍스트(user / post / engagement / fanout / feed)는 아래 계층을 갖는다.

```
domain/        엔티티·도메인 규칙. 어떤 프레임워크에도 의존하지 않는 순수 Java
application/   유스케이스 구현 + 포트 정의
  port/in/     유스케이스 인터페이스 (컨트롤러가 호출)
  port/out/    저장소·캐시·이벤트 발행 인터페이스 (어댑터가 구현)
adapter/
  in/web/      REST 컨트롤러 (port/in 호출)
  in/messaging/ Kafka 컨슈머 (port/in 호출)
  out/         JPA·Redis·Kafka 구현체 (port/out 구현)
```

- 의존성 방향은 항상 **바깥 → 안** (`adapter → application → domain`).
- 컨텍스트 간에는 **다른 컨텍스트의 port/in 또는 이벤트**를 통해서만 접근한다. adapter나 domain을 직접 참조하지 않는다.
- 이 규칙은 ArchUnit 테스트로 강제한다 → 나중에 컨텍스트를 마이크로서비스로 추출할 때 경계가 이미 깨끗하다.
