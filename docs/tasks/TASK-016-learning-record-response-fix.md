# TASK-016: 학습 기록 응답 보완 (작성일·표시용 순번 추가)

## Status

In Review

## Purpose

`TASK-014`로 구현된 학습 기록 API에서 사용자가 실제 사용 중 발견한 두 가지 응답 누락을 보완한다: (1) `LearningRecordResponse`에 작성일(`createdAt`)이 빠져 있어 조회가 불가능하고, (2) 학생별 학습 기록 목록에 사람이 읽기 좋은 순번이 없다.

## Background

`TASK-014` 스펙(`docs/tasks/TASK-014-learning-record-management.md`)의 `LearningRecordResponse`는 `{ id, studentId, title, content }`만 정의했다 — `createdAt` 내림차순 정렬 규칙(같은 문서 Business Rules)은 있지만 정작 그 값을 응답에 노출하지 않는 설계 누락이었다. 사용자가 실제로 학습 기록을 조회해보고 이 문제와 함께 "번호를 1부터 순차로 보고 싶다"는 요청을 전달했다.

두 번째 요청은 실제 식별자(`id`)를 순차 정수로 바꾸는 것으로 오해될 여지가 있어 사용자에게 직접 확인했다 — `id`는 `ADR-002`(TSID)를 그대로 유지하고, 목록 조회 응답에만 표시용 순번(`no`)을 추가하는 것으로 확정했다. `ADR-002`는 변경하지 않는다.

## Related Documents and Requirement IDs

- `docs/tasks/TASK-014-learning-record-management.md`(원 스펙, 이 TASK가 보완하는 대상)
- `docs/adr/ADR-002-tsid-as-identifier.md`(식별자 정책 — 변경 없음, `id`는 그대로 TSID 유지)

## Scope

### `createdAt` 노출

- `learningrecord/domain/LearningRecord.kt`: `createdAt: Instant` 필드를 추가한다. `record(...)` factory는 `Instant.now()`로 초기화하고, `reconstitute(...)`는 파라미터로 받는다(다른 필드와 동일한 패턴).
- `learningrecord/infrastructure/LearningRecordRepositoryAdapter.kt`: `toDomain()`에서 `LearningRecordJpaEntity.createdAt`(`BaseEntity`가 `@CreatedDate`로 이미 관리 중)을 그대로 전달한다. `toEntity()`는 변경하지 않는다 — `createdAt`은 `BaseEntity`의 JPA Auditing(`@CreatedDate`)이 저장 시점에 채우므로 도메인에서 엔티티로 역방향 전달할 필요가 없다.
- `learningrecord/presentation/LearningRecordDtos.kt`: `LearningRecordResponse`에 `createdAt: Instant` 필드를 추가하고 `from(...)`에서 매핑한다.

### 표시용 순번(`no`) 추가

- `LearningRecordResponse`에 `no: Int? = null`(nullable, 기본값 null)을 추가한다 — 목록 조회가 아닌 단건 응답(`POST`/`GET /{id}`/`PATCH`)에서는 값을 채우지 않는다.
- `learningrecord/presentation/LearningRecordController.kt`의 `list(studentId)`에서만 `no`를 계산해 채운다: 서비스가 반환하는 목록은 `createdAt` 내림차순(최신이 0번 인덱스)이므로, `no = 목록 크기 - 인덱스`로 계산해 가장 오래된 기록이 `no=1`, 가장 최근 기록이 가장 큰 번호가 되도록 매핑한다.
- `no`는 DB에 저장하지 않는다 — 매 조회 시 그 학생의 현재 목록을 기준으로 계산되는 값이며(다른 학생과 무관, 삭제 기능이 없으므로 항상 안정적이다), 별도 컬럼이나 시퀀스를 두지 않는다.

## Out of Scope

- `id`(TSID) 자체를 순차 정수로 바꾸는 것 — `ADR-002` 유지, 위 Background 참고.
- 다른 도메인(Attendance/Homework/Schedule 등)의 응답에 `createdAt`/순번을 추가하는 것 — 이번 요청은 학습 기록에 한정되며, 다른 도메인에 동일 패턴이 필요한지는 각각 별도로 판단한다.
- `updatedAt` 노출 — 사용자가 요청한 것은 작성일뿐이다.

## Functional Scenarios

- 학습 기록을 등록/조회/수정하면 응답에 `createdAt`이 포함된다.
- 특정 학생의 학습 기록 목록을 조회하면 각 항목에 `no`가 포함되고, 가장 오래된 기록이 `no=1`부터 시작해 최신 기록일수록 큰 번호를 가진다.
- 단건 조회(`GET /{id}`)와 등록/수정 응답에는 `no`가 포함되지 않는다(또는 `null`).

## Business Rules

- `no`는 저장되는 값이 아니라 목록 조회 시점에 계산되는 표시 전용 값이다 — 다른 학생의 기록과 번호가 섞이지 않으며, 학습 기록에는 삭제 기능이 없으므로(`TASK-014` Out of Scope) 시간이 지나도 같은 학생 내에서 번호가 밀리지 않는다.
- `id`(TSID)와 정렬 기준(`createdAt` 내림차순)은 변경하지 않는다.

## API Changes

- `LearningRecordResponse`: `{ "id": number, "studentId": number, "title": string, "content": string, "createdAt": string(ISO-8601), "no": number | null }`
- 엔드포인트 목록(`POST`/`GET`/`GET /{id}`/`PATCH`) 자체는 변경 없음 — 응답 바디에 위 두 필드만 추가된다.

## Domain Impact

`LearningRecord` Aggregate에 `createdAt` 필드가 추가된다. `no`는 도메인 모델에 속하지 않는 순수 표현(Presentation) 계층의 계산값이다.

## Database Impact

없음 — `learning_records` 테이블은 `V8` 마이그레이션에서 이미 `created_at` 컬럼을 갖고 있다(`BaseEntity`). 신규 마이그레이션 불필요.

## Exception and Error Handling

추가되는 예외 케이스 없음 — 기존 `TASK-014`의 예외 처리를 그대로 따른다.

## Test Scenarios

- `LearningRecordTest`: `record()`로 생성한 인스턴스의 `createdAt`이 채워지는지 확인.
- `LearningRecordApiTests`: 등록/조회/수정 응답에 `createdAt`이 포함되는지 확인, 학생별 목록 조회 시 `no`가 가장 오래된 기록부터 1, 2, 3...으로 부여되고 정렬 순서(내림차순)는 그대로 유지되는지 확인, 단건 응답에 `no`가 없는지(`null`) 확인.

## Acceptance Criteria

- 위 API 응답 변경이 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- 기존 `LearningRecordApiTests`의 다른 케이스(권한, 404, 400 등)에 회귀가 없다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- 다른 도메인(Attendance/Homework 등)도 동일하게 `createdAt`이 응답에 빠져 있는지는 이번 TASK에서 조사하지 않았다 — 필요해지면 별도로 확인.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`(영향 없음 — 유지 확인용으로만 참조)
