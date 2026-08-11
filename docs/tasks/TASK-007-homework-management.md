# TASK-007: 숙제(Homework) 관리 도메인 구현

## Status

Ready

## Purpose

`ADR-007`의 결정에 따라 `Homework` Aggregate를 구현한다. 선생님이 수업 또는 학생 단위로 학생에게 숙제를 부여·조회·수정·삭제하고, 제출/수행 상태를 확인할 수 있도록 한다.

## Background

`TASK-006`(Attendance)이 완료되어 있고, `ARCHITECTURE.md` §8 Deferred Decision #1(Aggregate 경계)이 `ADR-007`로 완전히 해소되었다 — Homework는 `studentId`를 필수로, `lessonId`는 nullable로 참조하는 독립 Aggregate이다.

숙제의 필드 구성(제목만)과 상태값(`ASSIGNED`/`SUBMITTED`/`GRADED`)은 `REQUIREMENTS.md`/`DOMAIN_MODEL.md`에 명시되지 않았던 항목이며, 이번 작업을 시작하며 사용자가 직접 확정했다(`TASK-006`이 출결 상태값을 확정한 것과 동일한 방식 — 별도 ADR 없이 이 TASK 문서에 기록).

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.5 숙제 관리
- `docs/DOMAIN_MODEL.md` 3.6 숙제(Homework)
- `docs/USER_ROLES.md` 3.5 숙제 관리
- `docs/adr/ADR-002-tsid-as-identifier.md`(ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md`(공통 404 처리)
- `docs/adr/ADR-005-query-strategy.md`(연관관계 금지, QueryDSL)
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`(Aggregate 경계, 이 작업이 실제 적용 사례)

## Users and Permissions

`USER_ROLES.md` 3.5를 그대로 적용한다:

| 역할 | 권한 |
|------|------|
| 관리자 | 서술 없음 — `USER_ROLES.md` 4장 미정 사항. **이번 작업에서는 기본 거부로 구현한다**(`TASK-005`가 서술 없는 권한을 기본 거부로 처리한 것과 동일한 원칙). |
| 선생님 | 수업 또는 학생 단위 숙제 등록, 조회, 수정, 삭제 / 숙제 제출·수행 상태 확인. `TASK-006`과 동일하게 "담당 수업" 스코핑 없이 전체 접근을 허용한다(`ARCHITECTURE.md` §6 미정 사항 "담당 학생/담당 반 스코핑"이 해소될 때까지 유지, 사용자가 `TASK-006`에서 확정한 방침을 그대로 확장 적용). |
| 학생 | 서술상 "본인에게 부여된 숙제 조회" 권한이 있으나, **이번 작업에서는 구현하지 않는다** — User-Student 연결 방식이 미정이라 `TASK-006`과 동일한 이유로 제외한다. |
| 학부모 | 서술상 "자녀의 숙제 현황 조회" 권한이 있으나, 같은 이유로 **이번 작업에서는 구현하지 않는다**. |

## Preconditions

`TASK-002`(Student), `TASK-003`(User + 인증/인가), `TASK-005`(Classroom/Lesson), `TASK-006`(Attendance, 동일 패턴 선례), `ADR-007`(Accepted)이 존재해야 한다.

## Scope

### Homework 도메인

- `homework/domain/Homework.kt` — private 생성자 + `assign(studentId, lessonId, title)` / `reconstitute(...)` companion factory. TSID `Long` id, factory에서 직접 할당(`ADR-002`). 새로 생성된 숙제는 상태 `ASSIGNED`로 시작한다(구현 시 채택한 가정).
- 필드: `id`, `studentId: Long`(필수), `lessonId: Long?`(nullable), `title: String`, `status`(`ASSIGNED`/`SUBMITTED`/`GRADED`).
- 도메인 메서드: `updateTitle(newTitle)`, `updateStatus(newStatus)`.
- `homework/domain/HomeworkStatus.kt`, `homework/domain/HomeworkNotFoundException.kt`(`common.domain.AggregateNotFoundException` 상속, `ADR-003` 공통 패턴), `homework/domain/HomeworkRepository.kt`(포트) — `save`, `findById`, `findAll`, `findAllByStudentId`, `deleteById`.
- `homework/infrastructure/HomeworkJpaEntity.kt` — `BaseEntity` 상속. `student_id`는 NOT NULL, `lesson_id`는 nullable 컬럼(연관관계 아님, `ARCHITECTURE.md` §6.1).
- `homework/infrastructure/HomeworkJpaRepository.kt`, `HomeworkRepositoryAdapter.kt`.
- `homework/application/HomeworkService.kt`:
  - `assign(studentId, lessonId, title)`: `studentId`가 실제 존재하는 `Student`인지 `StudentRepository`로 검증. `lessonId`가 주어진 경우 실제 존재하는 `Lesson`인지 `LessonRepository`로 검증하고, `studentId`가 해당 `Lesson.classId`의 `Classroom.studentIds`에 배정되어 있는지 `ClassroomRepository`로 검증(`TASK-006`의 출결 배정 검증 규칙을 동일하게 확장 적용 — 아래 Business Rules 참고). `lessonId`가 없으면(학생 단위 부여) 이 검증을 생략한다.
  - `updateTitle(id, newTitle)`, `updateStatus(id, newStatus)`: id로 조회 후 변경.
  - `delete(id)`: id로 조회 후 삭제(존재하지 않으면 404).
  - `findById(id)`, `findAll()`, `findAllByStudentId(studentId)`.
- `homework/presentation/HomeworkController.kt`, `HomeworkDtos.kt`. 별도 `ExceptionHandler`는 만들지 않는다 — `common.presentation.GlobalExceptionHandler`가 `HomeworkNotFoundException`을 자동으로 404로 처리한다(`ADR-003`).

### 데이터베이스

- `V5__create_homeworks_table.sql`:

```sql
CREATE TABLE homeworks (
    id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    lesson_id BIGINT NULL,
    title VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
```

물리 외래 키는 두지 않는다 — `student_id`, `lesson_id`는 순수 id 값만 저장하고, 참조 대상 존재 여부는 Application Service에서 검증한다(`ARCHITECTURE.md` §6.1). `Attendance`와 달리 `(lesson_id, student_id)` 유니크 제약은 두지 않는다 — 같은 수업·학생 조합에 여러 숙제가 존재할 수 있다.

### 인가

- `SecurityConfig`에 다음을 추가한다: `authorize("/api/v1/homeworks/**", hasRole("TEACHER"))` — GET을 포함한 모든 메서드를 선생님 전용으로 제한한다(관리자도 서술이 없으므로 접근 불가, 위 Users and Permissions 참고).

## Out of Scope

- 학생/학부모의 "본인/자녀 숙제 조회" — User-Student 연결, 학부모-학생 연결 방식이 미정이라 구현 불가(`TASK-006`과 동일한 사유).
- 관리자의 숙제 접근(조회 포함) — `USER_ROLES.md`에 서술 없음, 이번에는 부여하지 않는다.
- "수업 단위 숙제 템플릿"(하나의 숙제를 여러 학생에게 일괄 부여) — `ADR-007`에서 근거 부족으로 명시적으로 도입하지 않기로 결정했다. 숙제는 항상 학생 1명 단위로 생성한다.
- 첨부파일, 점수, 피드백 등 `title` 외 추가 필드 — 요구사항에 근거 없어 제외.
- 필터링/페이지네이션(기간별 등) — `findAll`/`findAllByStudentId` 수준의 단순 목록 조회만 구현한다.

## Functional Scenarios

- 선생님이 학생 단위로(`lessonId` 없이) 숙제를 제목과 함께 등록하면 상태 `ASSIGNED`로 생성된다.
- 선생님이 수업 단위로(`lessonId` 포함) 숙제를 등록하면, 해당 학생이 그 수업의 반에 배정되어 있어야 생성된다 — 배정되지 않았으면 오류를 반환한다.
- 선생님이 숙제의 제목을 수정하거나 상태를 `SUBMITTED`/`GRADED`로 변경할 수 있다.
- 선생님이 숙제를 삭제할 수 있다.
- 존재하지 않는 `studentId`/`lessonId`/숙제 id로 요청하면 404를 반환한다.
- 선생님만 숙제 API에 접근할 수 있다. 관리자를 포함한 다른 역할 또는 미인증 사용자는 403/401을 반환한다.

## Business Rules

- 숙제 제목(`title`)은 공백일 수 없고 100자를 초과할 수 없다(`Student.register`/`Classroom.register`와 동일한 검증 패턴).
- 새로 생성된 숙제는 상태 `ASSIGNED`로 시작한다.
- `lessonId`가 주어진 숙제는 대상 학생이 해당 수업이 속한 반(`Classroom`)에 배정되어 있어야 한다 — `TASK-006`에서 사용자가 출결에 대해 확정한 동일한 규칙을 같은 관계 구조(`DOMAIN_MODEL.md` §4 "수업 또는 학생 ── 부여 ──> 숙제")에 근거해 확장 적용했다(아래 Open Questions 참고, 재확인 권장).
- `lessonId` 없이(학생 단위) 부여된 숙제는 이 검증을 적용하지 않는다.

## API Changes

- `POST /api/v1/homeworks` — 201, `{ "studentId": number, "lessonId": number | null, "title": string }` → `HomeworkResponse`
- `GET /api/v1/homeworks` — 200, `HomeworkResponse[]`(전체 목록)
- `GET /api/v1/homeworks?studentId={id}` — 200, `HomeworkResponse[]`(특정 학생의 숙제 목록)
- `GET /api/v1/homeworks/{id}` — 200 / 404
- `PATCH /api/v1/homeworks/{id}` — 200, `{ "title": string?, "status": "ASSIGNED" | "SUBMITTED" | "GRADED"? }`(둘 중 하나 이상 포함)
- `DELETE /api/v1/homeworks/{id}` — 204
- `HomeworkResponse`: `{ "id": number, "studentId": number, "lessonId": number | null, "title": string, "status": "ASSIGNED" | "SUBMITTED" | "GRADED" }`

## Domain Impact

`DOMAIN_MODEL.md` 3.6 숙제(Homework)의 `Homework` Aggregate를 신규 도입한다(`ADR-007`). `Homework`는 `Student`(`studentId`, 필수), `Lesson`(`lessonId`, nullable) Aggregate를 각각 id 값으로만 참조한다.

## Database Impact

`V5__create_homeworks_table.sql` — `homeworks` 테이블 신규 생성(위 Scope 참고).

## Exception and Error Handling

- 존재하지 않는 학생/수업/숙제 id → 404(`HomeworkNotFoundException`, 학생/수업은 `StudentNotFoundException`/`LessonNotFoundException` 재사용).
- `lessonId`가 주어졌는데 대상 학생이 그 수업의 반에 배정되지 않은 경우 → 400.
- 입력값 검증 실패(제목 공백/초과, `title`/`status` 둘 다 없는 PATCH 요청 등) → 400(Spring 기본 처리).

## Test Scenarios

- `HomeworkTest`(도메인 단위): 생성, 제목 변경, 상태 변경.
- `HomeworkApiTests`(MockMvc + Testcontainers): 학생 단위/수업 단위 등록, 배정되지 않은 학생에 대한 수업 단위 등록 시 400, 목록·상세 조회, 제목/상태 수정, 삭제, 존재하지 않는 id 404, TEACHER 외 역할(ADMIN 포함) 403, 미인증 401.

## Acceptance Criteria

- 위 API가 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `HomeworkNotFoundException`이 `AggregateNotFoundException`을 상속해 별도 `ExceptionHandler` 없이 404가 반환된다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- **배정 검증 규칙의 확장 적용**: `lessonId`가 주어진 숙제에 "배정된 학생만" 규칙을 적용한 것은 `TASK-006`의 결정을 동일 구조에 유추 적용한 것이다 — 실제로 이 규칙이 숙제에도 동일하게 적용되어야 하는지 재확인 권장.
- **관리자 접근 전면 배제**: `USER_ROLES.md`에 관리자 권한 서술이 전혀 없어 이번 작업은 관리자도 완전히 배제했다 — 운영상 관리자 조회가 필요하다면 `USER_ROLES.md` §4를 먼저 갱신해야 한다.
- **학생/학부모 접근**: User-Student, 학부모-학생 연결 방식이 확정되면 "본인/자녀 숙제 조회" API를 별도 작업으로 추가해야 한다.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`
