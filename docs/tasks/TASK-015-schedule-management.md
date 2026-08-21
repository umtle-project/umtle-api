# TASK-015: 일정(Schedule) 관리 도메인 구현

## Status

Ready

## Purpose

`DOMAIN_MODEL.md` 3.4/`REQUIREMENTS.md` 3.3의 결정에 따라 `Schedule` Aggregate를 구현한다. 관리자가 학원 운영 일정을 등록·조회·수정·삭제하고, 선생님이 이를 조회할 수 있도록 한다.

## Background

`TASK-005`(Class/Lesson), `TASK-003`(User + 인증/인가)이 완료되어 있다. `Schedule`은 `Attendance`/`Homework`/`LearningRecord`(`ADR-007`)와 마찬가지로 다른 Aggregate를 id 값으로만 참조하는 독립 Aggregate다 — 다만 이들과 달리 `studentId`가 없고, `DOMAIN_MODEL.md` 3.4 "일정은 특정 수업과 연결될 수 있다"에 따라 `lessonId`만 nullable로 참조한다. 이는 `ARCHITECTURE.md` §5의 "Aggregate 간에는 식별자로만 참조한다" 원칙을 그대로 확장 적용한 것이라 별도 ADR 없이 이 문서에 기록한다.

일정의 필드 구성(제목/설명/시작·종료 일시)과 선생님/학생·학부모 조회 범위는 `REQUIREMENTS.md`/`DOMAIN_MODEL.md`/`USER_ROLES.md`에 명시되지 않았던 항목이며, 이번 작업을 시작하며 사용자가 세션 중 직접 확정했다(`TASK-006`이 출결 상태값을, `TASK-007`이 숙제 필드를 확정한 것과 동일한 방식):

- **필드 구성**: `title`(제목, 필수) + `content`(설명, 선택) + `startAt`/`endAt`(시작·종료 일시, 둘 다 필수) + `lessonId`(연결된 수업, 선택)로 구성한다.
- **선생님 조회 범위**: `TASK-006`/`TASK-007`/`TASK-014`와 동일하게 "담당 수업" 스코핑 없이 전체 조회를 허용한다(`ARCHITECTURE.md` §8에서 파생된 "담당 학생/담당 반 스코핑" 미정 사항이 해소될 때까지 유지, `TASK-006`에서 확정한 방침을 그대로 확장 적용).
- **학생/학부모 조회**: `USER_ROLES.md` 3.3은 "본인/자녀와 관련된 일정 조회"를 명시하지만, `Schedule`은 `studentId`를 직접 갖지 않고 `lessonId`만 선택적으로 가지므로 "본인 관련 일정"을 판별하려면 `Lesson → Class.studentIds` 조인이 필요하다. 이 조인 스코핑은 별도 설계가 필요하다고 판단해 **이번 작업에서는 구현하지 않는다** — `TASK-014`가 학생/학부모 공개를 후속 작업으로 미룬 것과 동일한 패턴이다(아래 Out of Scope/Open Questions 참고).

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.3 일정 관리
- `docs/DOMAIN_MODEL.md` 3.4 일정(Schedule)
- `docs/USER_ROLES.md` 3.3 일정 관리
- `docs/adr/ADR-002-tsid-as-identifier.md`(ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md`(공통 404 처리)
- `docs/adr/ADR-005-query-strategy.md`(연관관계 금지, QueryDSL)
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`(id 참조 Aggregate 경계 선례)

## Users and Permissions

`USER_ROLES.md` 3.3을 그대로 적용하되, 학생/학부모는 이번 작업에서 제외한다(위 Background 참고):

| 역할 | 권한 |
|------|------|
| 관리자 | 학원 운영 일정 등록, 조회, 수정, 삭제 |
| 선생님 | 일정 조회. `TASK-006`/`TASK-007`/`TASK-014`와 동일하게 "담당 수업" 스코핑 없이 전체 일정 대상 조회를 허용한다(`ARCHITECTURE.md` §8 미정 사항이 해소될 때까지 유지). |
| 학생 | 서술상 "본인과 관련된 일정 조회" 권한이 있으나, `Lesson → Class.studentIds` 조인 스코핑 설계가 필요해 **이번 작업에서는 구현하지 않는다**(위 Background 참고). |
| 학부모 | 서술상 "자녀와 관련된 일정 조회" 권한이 있으나, 같은 이유로 **이번 작업에서는 구현하지 않는다**. |

## Preconditions

`TASK-002`(Student), `TASK-003`(User + 인증/인가), `TASK-005`(Class/Lesson)가 존재해야 한다.

## Scope

### Schedule 도메인

- `schedule/domain/Schedule.kt` — private 생성자 + `register(title, content, startAt, endAt, lessonId)` / `reconstitute(...)` companion factory. TSID `Long` id, factory에서 직접 할당(`ADR-002`).
- 필드: `id`, `title: String`(필수), `content: String?`(nullable), `startAt: LocalDateTime`(필수), `endAt: LocalDateTime`(필수), `lessonId: Long?`(nullable, 생성 후 불변).
- 도메인 메서드: `update(newTitle, newContent, newStartAt, newEndAt)` — 제목/설명/시작·종료 일시를 함께 교체한다. `lessonId`는 생성 시에만 정하며 수정 대상에 포함하지 않는다. 생성·수정 시 `endAt >= startAt`을 검증한다(아래 Business Rules 참고).
- `schedule/domain/ScheduleNotFoundException.kt`(`common.domain.AggregateNotFoundException` 상속, `ADR-003` 공통 패턴), `schedule/domain/ScheduleRepository.kt`(포트) — `save`, `findById`, `findAll`, `findAllByLessonId`, `deleteById`.
- `schedule/infrastructure/ScheduleJpaEntity.kt` — `BaseEntity` 상속. `lesson_id`는 nullable 컬럼(연관관계 아님, `ARCHITECTURE.md` §6.1).
- `schedule/infrastructure/ScheduleJpaRepository.kt`, `ScheduleRepositoryAdapter.kt`.
- `schedule/application/ScheduleService.kt`:
  - `register(title, content, startAt, endAt, lessonId)`: `lessonId`가 주어진 경우 실제 존재하는 `Lesson`인지 `LessonRepository`로 검증(존재하지 않으면 404). `lessonId`가 없으면 이 검증을 생략한다. `studentId`가 없으므로 `Homework`처럼 "배정된 학생인지" 검증하는 절차는 없다.
  - `update(id, title, content, startAt, endAt)`: id로 조회 후 `update` 호출.
  - `delete(id)`: id로 조회 후 삭제(존재하지 않으면 404).
  - `findById(id)`, `findAll()`, `findAllByLessonId(lessonId)`.
- `schedule/presentation/ScheduleController.kt`, `ScheduleDtos.kt`. 별도 `ExceptionHandler`는 만들지 않는다 — `common.presentation.GlobalExceptionHandler`가 `ScheduleNotFoundException`을 자동으로 404로 처리한다(`ADR-003`).

### 데이터베이스

- `V9__create_schedules_table.sql`:

```sql
CREATE TABLE schedules (
    id BIGINT NOT NULL,
    title VARCHAR(100) NOT NULL,
    content VARCHAR(2000) NULL,
    start_at DATETIME(6) NOT NULL,
    end_at DATETIME(6) NOT NULL,
    lesson_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
```

물리 외래 키는 두지 않는다 — `lesson_id`는 순수 id 값만 저장하고, 참조 대상 존재 여부는 Application Service에서 검증한다(`ARCHITECTURE.md` §6.1).

### 인가

- `SecurityConfig`에 다음을 추가한다:
  - `authorize(HttpMethod.POST, "/api/v1/schedules", hasRole("ADMIN"))`
  - `authorize(HttpMethod.PATCH, "/api/v1/schedules/**", hasRole("ADMIN"))`
  - `authorize(HttpMethod.DELETE, "/api/v1/schedules/**", hasRole("ADMIN"))`
  - `authorize(HttpMethod.GET, "/api/v1/schedules", hasAnyRole("ADMIN", "TEACHER"))`
  - `authorize(HttpMethod.GET, "/api/v1/schedules/**", hasAnyRole("ADMIN", "TEACHER"))`

## Out of Scope

- 학생/학부모의 "본인/자녀 관련 일정 조회" — `Lesson → Class.studentIds` 조인 스코핑 설계가 필요해 이번 작업에서는 구현하지 않는다(위 Background 참고). 필요해지면 별도 작업에서 조인 전략을 결정한다.
- "담당 수업" 스코핑 — 반-선생님 배정 기준의 스코핑 정책이 프로젝트 전체에서 미정(`ARCHITECTURE.md` §8). `TASK-006`/`TASK-007`/`TASK-014`와 동일하게 선생님은 전체 일정 대상 조회를 허용한다.
- 반복 일정(recurring schedule) — `REQUIREMENTS.md`에 근거 없어 제외. 일정은 항상 단일 기간(`startAt`~`endAt`)으로만 등록한다.
- 카테고리/장소/알림 등 `title`/`content`/`startAt`/`endAt`/`lessonId` 외 추가 필드 — 요구사항에 근거 없어 제외.
- 필터링/페이지네이션(기간별 등) — `findAll`/`findAllByLessonId` 수준의 단순 목록 조회만 구현한다.

## Functional Scenarios

- 관리자가 제목, 시작·종료 일시로 일정을 등록하면 저장된다(설명, 연결 수업은 선택).
- 관리자가 수업과 연결된 일정을 등록하면, 해당 `lessonId`가 실제 존재해야 생성된다 — 존재하지 않으면 404를 반환한다.
- 관리자가 일정의 제목/설명/시작·종료 일시를 수정하면 반영된다.
- 관리자가 일정을 삭제할 수 있다.
- 관리자 또는 선생님이 전체 일정 목록을 조회하거나, 특정 `lessonId`로 필터링해 조회할 수 있다.
- 관리자 또는 선생님이 일정 상세를 조회할 수 있다.
- 존재하지 않는 일정 id로 상세 조회/수정/삭제를 시도하면 404를 반환한다.
- 제목이 비어있거나 길이 제한을 초과하거나, `endAt`이 `startAt`보다 이르면 400을 반환한다.
- 선생님이 등록(`POST`)/수정(`PATCH`)/삭제(`DELETE`)를 시도하면 403을 반환한다(선생님은 조회만 가능).
- 학생/학부모 역할 또는 미인증 사용자가 일정 API를 호출하면 403/401을 반환한다.

## Business Rules

- 일정 제목(`title`)은 공백일 수 없고 100자를 초과할 수 없다(`Homework.title`과 동일한 검증 패턴).
- 일정 설명(`content`)은 존재하면 2000자를 초과할 수 없다(선택 필드).
- `endAt`은 `startAt`보다 빠를 수 없다(`startAt <= endAt`) — 생성·수정 시 모두 검증한다.
- `lessonId`가 주어진 일정은 해당 `Lesson`이 실제로 존재해야 한다. `Homework`와 달리 특정 학생이 그 반에 배정되어 있는지는 검증하지 않는다 — `Schedule`은 `studentId`를 갖지 않기 때문이다.
- `lessonId`는 생성 시에만 지정하며 수정(`update`) 대상에 포함하지 않는다 — 연결된 수업을 바꾸려면 일정을 삭제하고 새로 등록한다(구현 시 채택하는 가정, 아래 Open Questions 참고).

## API Changes

- `POST /api/v1/schedules` — 201, `{ "title": string, "content": string | null, "startAt": string(ISO-8601), "endAt": string(ISO-8601), "lessonId": number | null }` → `ScheduleResponse`
- `GET /api/v1/schedules` (+ optional `?lessonId={id}`) — 200, `ScheduleResponse[]`(`lessonId` 생략 시 전체 목록, 지정 시 해당 수업과 연결된 일정만)
- `GET /api/v1/schedules/{id}` — 200 / 404
- `PATCH /api/v1/schedules/{id}` — 200, `{ "title": string, "content": string | null, "startAt": string, "endAt": string }`(`lessonId` 제외 전부 필수) → `ScheduleResponse`
- `DELETE /api/v1/schedules/{id}` — 204
- `ScheduleResponse`: `{ "id": number, "title": string, "content": string | null, "startAt": string, "endAt": string, "lessonId": number | null }`

## Domain Impact

`DOMAIN_MODEL.md` 3.4 일정(Schedule)의 `Schedule` Aggregate를 신규 도입한다. `Schedule`은 `Lesson`(`lessonId`, nullable)을 id 값으로만 참조하며, `Student`는 참조하지 않는다.

## Database Impact

`V9__create_schedules_table.sql` — `schedules` 테이블 신규 생성(위 Scope 참고).

## Exception and Error Handling

- 존재하지 않는 `lessonId`로 등록 시도 → 404(`LessonNotFoundException`, 기존 재사용).
- 존재하지 않는 일정 id로 상세 조회/수정/삭제 → 404(`ScheduleNotFoundException`).
- `title` 공백/길이 초과, `content` 길이 초과, `startAt`/`endAt` 누락, `endAt < startAt` → 400(도메인 검증 및 Spring 기본 처리).
- 선생님의 등록/수정/삭제 시도, 학생/학부모/미인증 사용자의 API 호출 → 403/401.

## Test Scenarios

- `ScheduleTest`(도메인 단위): 생성, 제목/설명/시작·종료 일시 수정, `endAt < startAt`일 때 생성·수정 각각 예외, 제목 공백·길이 초과 시 예외.
- `ScheduleApiTests`(MockMvc + Testcontainers): 일정 등록(관리자, `lessonId` 있음/없음 각각), 존재하지 않는 `lessonId`로 등록 시 404, 목록 조회(전체/`lessonId` 필터링), 상세 조회, 수정, 삭제, 존재하지 않는 일정 id 404, 검증 실패 400, 선생님의 등록/수정/삭제 403, 학생/학부모/미인증 403/401.

## Acceptance Criteria

- 위 API가 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `ScheduleNotFoundException`이 `AggregateNotFoundException`을 상속해 별도 `ExceptionHandler` 없이 404가 반환된다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- **학생/학부모 조회 범위**: `Lesson → Class.studentIds` 조인으로 "본인/자녀 관련 일정"을 판별하는 스코핑 로직을 별도 작업에서 설계해야 한다(위 Background 참고). 이때 `lessonId`가 없는 일정(학원 전체 운영 일정)을 학생/학부모에게 어떻게 노출할지(전체 공개 여부)도 함께 결정 필요.
- **"담당 수업" 스코핑**: 반-선생님 배정 기준이 확정되면 선생님의 조회 범위를 제한하는 별도 작업이 필요하다(`TASK-006`/`TASK-007`/`TASK-013`/`TASK-014`와 동일한 미결 사항).
- **`lessonId` 수정 불가 정책**: 이번 작업은 생성 후 `lessonId`를 불변으로 가정했다 — 운영상 연결 수업을 바꿔야 하는 요구가 생기면 재검토 필요.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`
