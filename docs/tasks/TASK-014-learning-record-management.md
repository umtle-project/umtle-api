# TASK-014: 학습 기록(Learning Record) 관리 도메인 구현

## Status

Ready

## Purpose

`ADR-007`의 결정에 따라 `LearningRecord` Aggregate를 구현한다. 선생님이 학생별 학습 기록을 작성·조회·수정하고, 관리자는 학생별 학습 이력을 조회할 수 있도록 한다.

## Background

`TASK-002`(Student), `TASK-003`(User + 인증/인가)이 완료되어 있고, `ARCHITECTURE.md` §8 Deferred Decision #1(Aggregate 경계)이 `ADR-007`로 완전히 해소되었다 — `LearningRecord`는 `studentId`만 참조하는(`lessonId` 없음) 독립 Aggregate이다.

학습 기록의 필드 구성(제목+내용)과 학생/학부모 공개 여부는 `REQUIREMENTS.md`/`DOMAIN_MODEL.md`/`USER_ROLES.md`에 명시되지 않았던 항목이며, 이번 작업을 시작하며 사용자가 세션 중 직접 확정했다(`TASK-006`이 출결 상태값을, `TASK-007`이 숙제 필드를 확정한 것과 동일한 방식 — 별도 ADR 없이 이 TASK 문서에 기록):

- **필드 구성**: `title`(제목) + `content`(내용) 두 필드 모두 필수로 구성한다.
- **학생/학부모 공개**: 이번 작업에서는 비공개로 시작한다 — 학생/학부모에게 학습 기록을 노출하는 API는 이번에 구현하지 않는다. `USER_ROLES.md` 3.6/4장에 남아있는 "학습 기록의 학생·학부모 공개 범위" 미정 사항은 이번 작업으로 해소되지 않으며, 공개가 필요해지면 별도 작업에서 범위를 다시 결정한다.

`Homework`(`TASK-007`)와 달리 `LearningRecord`는 "전체 목록 조회" 요구사항이 문서에 없다(`USER_ROLES.md` 3.6은 "학생별 학습 이력 조회"로만 서술) — 이번 작업은 항상 `studentId` 단위 조회만 제공하고, `Homework`의 `findAll()`/전체 목록 API에 대응하는 기능은 두지 않는다.

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.6 학습 기록 관리
- `docs/DOMAIN_MODEL.md` 3.7 학습 기록(Learning Record)
- `docs/USER_ROLES.md` 3.6 학습 기록 관리
- `docs/adr/ADR-002-tsid-as-identifier.md`(ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md`(공통 404 처리)
- `docs/adr/ADR-005-query-strategy.md`(연관관계 금지, QueryDSL)
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`(Aggregate 경계, 이 작업이 실제 적용 사례)

## Users and Permissions

`USER_ROLES.md` 3.6을 그대로 적용한다:

| 역할 | 권한 |
|------|------|
| 관리자 | 학생별 학습 이력 조회(작성/수정 권한 없음) |
| 선생님 | 학생별 학습 기록 작성, 조회, 수정. `TASK-006`/`TASK-007`과 동일하게 "담당 학생" 스코핑 없이 전체 학생 대상 접근을 허용한다(`ARCHITECTURE.md` §6 미정 사항이 해소될 때까지 유지). |
| 학생 | 서술상 공개 여부가 미정이며, 이번 작업에서는 구현하지 않는다(위 Background 참고). |
| 학부모 | 서술상 공개 여부가 미정이며, 이번 작업에서는 구현하지 않는다(위 Background 참고). |

## Preconditions

`TASK-002`(Student), `TASK-003`(User + 인증/인가), `ADR-007`(Accepted)이 존재해야 한다.

## Scope

### LearningRecord 도메인

- `learningrecord/domain/LearningRecord.kt` — private 생성자 + `record(studentId, title, content)` / `reconstitute(...)` companion factory. TSID `Long` id, factory에서 직접 할당(`ADR-002`).
- 필드: `id`, `studentId: Long`(필수), `title: String`(필수), `content: String`(필수). `lessonId`는 두지 않는다(`ADR-007` Decision 4).
- 도메인 메서드: `update(newTitle, newContent)` — 제목과 내용을 함께 교체한다. `title`/`content` 모두 필수 파라미터이므로(nullable이 아님) `TASK-013` 리뷰에서 발견된 "부분 수정 시 데이터 유실" 문제가 구조적으로 발생하지 않는다 — 요청에 필드가 하나라도 없으면 Bean Validation 단계에서 400으로 거부된다.
- `learningrecord/domain/LearningRecordNotFoundException.kt`(`common.domain.AggregateNotFoundException` 상속, `ADR-003` 공통 패턴), `learningrecord/domain/LearningRecordRepository.kt`(포트) — `save`, `findById`, `findAllByStudentId`.
- `learningrecord/infrastructure/LearningRecordJpaEntity.kt` — `BaseEntity` 상속. `student_id`는 NOT NULL 컬럼(연관관계 아님, `ARCHITECTURE.md` §6.1).
- `learningrecord/infrastructure/LearningRecordJpaRepository.kt`, `LearningRecordRepositoryAdapter.kt`.
- `learningrecord/application/LearningRecordService.kt`:
  - `record(studentId, title, content)`: `studentId`가 실제 존재하는 `Student`인지 `StudentRepository`로 검증 후 생성.
  - `update(id, title, content)`: id로 조회 후 `update` 호출.
  - `findById(id)`.
  - `findAllByStudentId(studentId)`: `studentId`가 실제 존재하는 `Student`인지 검증 후 목록 반환(`createdAt` 내림차순 — 아래 Business Rules 참고).
- `learningrecord/presentation/LearningRecordController.kt`, `LearningRecordDtos.kt`. 별도 `ExceptionHandler`는 만들지 않는다 — `common.presentation.GlobalExceptionHandler`가 `LearningRecordNotFoundException`을 자동으로 404로 처리한다(`ADR-003`).

### 데이터베이스

- `V8__create_learning_records_table.sql`:

```sql
CREATE TABLE learning_records (
    id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
```

물리 외래 키는 두지 않는다 — `student_id`는 순수 id 값만 저장하고, 참조 대상 존재 여부는 Application Service에서 검증한다(`ARCHITECTURE.md` §6.1).

### 인가

- `SecurityConfig`에 다음을 추가한다:
  - `authorize(HttpMethod.POST, "/api/v1/learning-records", hasRole("TEACHER"))`
  - `authorize(HttpMethod.PATCH, "/api/v1/learning-records/**", hasRole("TEACHER"))`
  - `authorize(HttpMethod.GET, "/api/v1/learning-records", hasAnyRole("ADMIN", "TEACHER"))`
  - `authorize(HttpMethod.GET, "/api/v1/learning-records/**", hasAnyRole("ADMIN", "TEACHER"))`

## Out of Scope

- 학생/학부모에게 학습 기록을 공개하는 API — 이번 작업에서는 비공개로 시작한다(위 Background 참고). 공개가 필요해지면 별도 작업에서 범위를 결정한다.
- "담당 학생" 스코핑 — 반-선생님 배정 기준이 프로젝트 전체에서 미정(`DOMAIN_MODEL.md` §6). `TASK-006`/`TASK-007`과 동일하게 전체 학생 대상 접근을 허용한다.
- 전체 학습 기록 목록 조회(`studentId` 없는 목록) — `USER_ROLES.md`에 근거 없음. 항상 `studentId` 단위로만 조회한다.
- 삭제(DELETE) 기능 — `REQUIREMENTS.md` 3.6/`USER_ROLES.md` 3.6 모두 "작성·조회·수정"만 명시하고 삭제는 언급하지 않는다.
- 카테고리/과목/첨부파일/점수 등 `title`/`content` 외 추가 필드 — 요구사항에 근거 없어 제외.
- 필터링/페이지네이션(기간별 등) — `findAllByStudentId` 수준의 단순 목록 조회만 구현한다.

## Functional Scenarios

- 선생님이 학생 id, 제목, 내용으로 학습 기록을 작성하면 저장된다.
- 선생님이 학습 기록의 제목과 내용을 함께 수정하면 반영된다.
- 선생님 또는 관리자가 특정 학생의 학습 기록 목록을 조회하면 해당 학생의 기록만 `createdAt` 내림차순으로 반환된다.
- 선생님 또는 관리자가 학습 기록 상세를 조회할 수 있다.
- 존재하지 않는 `studentId`로 작성/목록 조회를 시도하면 404를 반환한다.
- 존재하지 않는 학습 기록 id로 상세 조회/수정을 시도하면 404를 반환한다.
- 제목/내용이 비어있거나 길이 제한을 초과하면 400을 반환한다.
- 관리자가 작성(`POST`) 또는 수정(`PATCH`)을 시도하면 403을 반환한다(관리자는 조회만 가능).
- 학생/학부모 역할 또는 미인증 사용자가 학습 기록 API를 호출하면 403/401을 반환한다.

## Business Rules

- 학습 기록 제목(`title`)은 공백일 수 없고 100자를 초과할 수 없다(`Homework.title`과 동일한 검증 패턴).
- 학습 기록 내용(`content`)은 공백일 수 없고 2000자를 초과할 수 없다.
- 수정(`update`)은 제목과 내용을 항상 함께 전달한다 — 부분 수정 개념을 두지 않는다(위 Scope 참고).
- 학생별 목록 조회는 `createdAt` 내림차순으로 정렬한다(구현 시 채택하는 가정 — 최근 기록이 먼저 보이도록. `TASK-013`의 상세 조회 "최근 5건" 정렬 기준과 동일한 근거).

## API Changes

- `POST /api/v1/learning-records` — 201, `{ "studentId": number, "title": string, "content": string }` → `LearningRecordResponse`
- `GET /api/v1/learning-records?studentId={id}` — 200, `LearningRecordResponse[]`(해당 학생의 학습 기록 목록, `createdAt` 내림차순). `studentId`는 필수 쿼리 파라미터다 — 생략 시 400을 반환한다(`Homework`의 "전체 목록" 기본 동작과 달리, 이 도메인은 항상 학생 단위 조회만 지원하기 때문).
- `GET /api/v1/learning-records/{id}` — 200 / 404
- `PATCH /api/v1/learning-records/{id}` — 200, `{ "title": string, "content": string }`(둘 다 필수) → `LearningRecordResponse`
- `LearningRecordResponse`: `{ "id": number, "studentId": number, "title": string, "content": string }`

## Domain Impact

`DOMAIN_MODEL.md` 3.7 학습 기록(Learning Record)의 `LearningRecord` Aggregate를 신규 도입한다(`ADR-007`). `LearningRecord`는 `Student`(`studentId`, 필수)를 id 값으로만 참조하며, `Lesson`은 참조하지 않는다.

## Database Impact

`V8__create_learning_records_table.sql` — `learning_records` 테이블 신규 생성(위 Scope 참고).

## Exception and Error Handling

- 존재하지 않는 학생 id로 작성/목록 조회 → 404(`StudentNotFoundException`, 기존 재사용).
- 존재하지 않는 학습 기록 id로 상세 조회/수정 → 404(`LearningRecordNotFoundException`).
- `title`/`content` 공백 또는 길이 초과, `studentId` 쿼리 파라미터 누락 → 400(Spring 기본 처리).
- 관리자의 작성/수정 시도, 학생/학부모/미인증 사용자의 API 호출 → 403/401.

## Test Scenarios

- `LearningRecordTest`(도메인 단위): 생성, 제목/내용 수정, 제목/내용 각각 공백·길이 초과 시 예외.
- `LearningRecordApiTests`(MockMvc + Testcontainers): 학습 기록 작성(선생님), 목록 조회(선생님/관리자 각각, `studentId` 기준 필터링 및 정렬 확인), 상세 조회, 제목/내용 수정, 존재하지 않는 `studentId`/기록 id 404, 제목/내용 검증 실패 400, 관리자의 작성/수정 시도 403, 학생/학부모/미인증 403/401.

## Acceptance Criteria

- 위 API가 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `LearningRecordNotFoundException`이 `AggregateNotFoundException`을 상속해 별도 `ExceptionHandler` 없이 404가 반환된다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- **학생/학부모 공개 범위**: 이번 작업은 비공개로 시작하기로 사용자가 확정했다 — 공개가 필요해지면 범위(전체 공개/필드별 공개 등)를 별도 작업에서 재결정해야 한다.
- **"담당 학생" 스코핑**: 반-선생님 배정 기준이 확정되면 선생님의 조회 범위를 제한하는 별도 작업이 필요하다(`TASK-006`/`TASK-007`/`TASK-013`과 동일한 미결 사항).
- **목록 정렬 기준(`createdAt`)**: `TASK-013`과 동일하게 레코드 생성 시각 기준 정렬을 채택했다 — 학습 기록에 별도의 "작성 일자"/"수업 일자" 개념이 필요해지면 재검토 필요.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`
