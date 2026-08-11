# TASK-006: 출결(Attendance) 관리 도메인 구현

## Status

Ready

## Purpose

`ADR-007`의 결정에 따라 `Attendance` Aggregate를 구현한다. 특정 수업(Lesson)에서 특정 학생(Student)의 출결 상태를 선생님이 기록·수정하고, 관리자가 전체를 조회할 수 있도록 한다.

## Background

`TASK-005`(Class/Lesson)가 완료되어 있고, `ARCHITECTURE.md` §8 Deferred Decision #1(Aggregate 경계)이 `ADR-007`로 완전히 해소되었다 — Attendance는 Lesson/Student 어느 쪽에도 종속되지 않는 독립 Aggregate이며, `lessonId`와 `studentId`를 모두 필수로 참조한다.

출결 상태값(`PRESENT`/`LATE`/`ABSENT`/`EXCUSED`)은 `REQUIREMENTS.md` §5에 미정으로 남아 있던 항목이며, 이번 작업을 시작하며 사용자가 직접 확정했다(`ADR-006`이 Lesson 상태값을 확정한 것과 동일한 방식 — 별도 ADR 없이 이 TASK 문서에 기록).

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.4 출결 관리
- `docs/DOMAIN_MODEL.md` 3.5 출결(Attendance)
- `docs/USER_ROLES.md` 3.4 출결 관리
- `docs/adr/ADR-002-tsid-as-identifier.md`(ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md`(공통 404 처리)
- `docs/adr/ADR-005-query-strategy.md`(연관관계 금지, QueryDSL)
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`(Aggregate 경계, 이 작업이 실제 적용 사례)

## Users and Permissions

`USER_ROLES.md` 3.4를 그대로 적용한다:

| 역할 | 권한 |
|------|------|
| 관리자 | 전체 출결 기록 조회 |
| 선생님 | 수업별 학생 출결 상태 기록(생성) 및 수정. 기록/수정 작업에 필요한 범위의 조회도 함께 허용한다(아래 Open Questions 참고). |
| 학생 | 서술상 "본인의 출결 기록 조회" 권한이 있으나, **이번 작업에서는 구현하지 않는다** — User(로그인 계정)와 Student(학생 Aggregate)의 연결 방식이 아직 미정이라 "본인"을 판별할 수 없다(`DOMAIN_MODEL.md` §6 미정 사항). |
| 학부모 | 서술상 "자녀의 출결 기록 조회" 권한이 있으나, 같은 이유로 **이번 작업에서는 구현하지 않는다** — 학부모-학생 연결 방식도 미정이다. |

`USER_ROLES.md`는 관리자의 등록/수정 권한, 선생님의 목록 조회 권한을 명시적으로 서술하지 않는다 — 이번 작업은 "관리자=조회만", "선생님=기록·수정·조회"로 구현한다. 선생님의 조회 범위는 "담당 수업" 스코핑 없이 전체 조회를 허용하기로 사용자가 확정했다(`ARCHITECTURE.md` §6 미정 사항인 "담당 학생/담당 반 스코핑"이 해소될 때까지는 이 방식을 유지한다).

## Preconditions

`TASK-002`(Student), `TASK-003`(User + 인증/인가), `TASK-005`(Classroom/Lesson), `ADR-007`(Accepted)이 존재해야 한다.

## Scope

### Attendance 도메인

- `attendance/domain/Attendance.kt` — `Lesson.kt`/`Student.kt`와 동일한 패턴(private 생성자 + `record(lessonId, studentId, status)` / `reconstitute(...)` companion factory). TSID `Long` id, factory에서 직접 할당(`ADR-002`).
- 필드: `id`, `lessonId: Long`, `studentId: Long`, `status`(`PRESENT`/`LATE`/`ABSENT`/`EXCUSED`).
- 도메인 메서드: `updateStatus(newStatus)`.
- `attendance/domain/AttendanceStatus.kt`, `attendance/domain/AttendanceNotFoundException.kt`(`common.domain.AggregateNotFoundException` 상속, `ADR-003` 공통 패턴), `attendance/domain/AttendanceRepository.kt`(포트) — `findById`, `findAllByLessonId`, `findAll`, `existsByLessonIdAndStudentId`.
- `attendance/infrastructure/AttendanceJpaEntity.kt` — `BaseEntity` 상속. `lesson_id`, `student_id`는 일반 `Long` 컬럼(연관관계 아님, `ARCHITECTURE.md` §6.1).
- `attendance/infrastructure/AttendanceJpaRepository.kt`, `AttendanceRepositoryAdapter.kt`.
- `attendance/application/AttendanceService.kt`:
  - `record(lessonId, studentId, status)`: `lessonId`가 실제 존재하는 `Lesson`인지 `LessonRepository`로 검증, `studentId`가 실제 존재하는 `Student`인지 `StudentRepository`로 검증, `studentId`가 해당 `Lesson.classId`의 `Classroom.studentIds`에 배정되어 있는지 `ClassroomRepository`로 검증(배정되지 않은 학생이면 예외). 이미 같은 `(lessonId, studentId)` 조합의 출결이 존재하면 예외(중복 생성 방지, 수정은 별도 API로).
  - `updateStatus(id, newStatus)`: id로 조회 후 상태 변경.
  - `findById(id)`, `findAllByLessonId(lessonId)`, `findAll()`(관리자 전체 조회용).
- `attendance/presentation/AttendanceController.kt`, `AttendanceDtos.kt`. 별도 `ExceptionHandler`는 만들지 않는다 — `common.presentation.GlobalExceptionHandler`가 `AttendanceNotFoundException`을 자동으로 404로 처리한다(`ADR-003`).

### 데이터베이스

- `V4__create_attendances_table.sql`:

```sql
CREATE TABLE attendances (
    id BIGINT NOT NULL,
    lesson_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_attendances_lesson_student UNIQUE (lesson_id, student_id)
);
```

물리 외래 키는 두지 않는다 — `lesson_id`, `student_id`는 순수 id 값만 저장하고, 참조 대상 존재 여부는 Application Service에서 검증한다(`ARCHITECTURE.md` §6.1).

### 인가

- `SecurityConfig`에 다음을 추가한다(기존 `GET /api/v1/students/**` → `hasAnyRole("ADMIN", "TEACHER")` 패턴과 동일):
  - `GET /api/v1/attendances/**`, `GET /api/v1/lessons/*/attendances` → `hasAnyRole("ADMIN", "TEACHER")`
  - `POST /api/v1/lessons/*/attendances`, `PATCH /api/v1/attendances/**` → `hasRole("TEACHER")`

## Out of Scope

- 학생/학부모의 "본인/자녀 출결 조회" — User-Student 연결, 학부모-학생 연결 방식이 미정이라 구현 불가(위 Users and Permissions 참고). 연결 방식이 확정되면 별도 작업으로 추가한다.
- 출결 기록 삭제 — `REQUIREMENTS.md` 3.4는 "기록하고 수정"만 요구하며 삭제를 요구하지 않는다.
- Homework/LearningRecord 연동 — 아직 구현되지 않은 도메인이다.
- 관리자의 출결 생성/수정 권한 — `USER_ROLES.md`에 서술 없음, 이번에는 조회만 부여한다.
- 필터링/페이지네이션(기간별, 학생별 등) — 현재 코드베이스에 확립된 패턴이 없어 `findAllByLessonId`/`findAll` 수준의 단순 목록 조회만 구현한다.

## Functional Scenarios

- 선생님이 특정 수업(`lessonId`)에 배정된 학생(`studentId`)의 출결을 상태와 함께 등록하면 생성된다.
- 배정되지 않은 학생의 출결을 등록하려 하면 오류를 반환한다.
- 이미 출결이 등록된 `(lessonId, studentId)` 조합에 다시 등록을 시도하면 오류를 반환한다 — 수정은 별도 API로 한다.
- 선생님이 기존 출결 레코드의 상태를 수정할 수 있다.
- 존재하지 않는 `lessonId`/`studentId`/출결 id로 요청하면 404를 반환한다.
- 관리자와 선생님은 출결 목록/상세를 조회할 수 있다. 그 외 역할 또는 미인증 사용자는 401/403을 반환한다.

## Business Rules

- 하나의 `(lessonId, studentId)` 조합에는 출결 레코드가 최대 1개만 존재한다(`uk_attendances_lesson_student`).
- 출결 대상 학생은 해당 수업이 속한 반(`Classroom`)에 배정되어 있어야 한다 — `REQUIREMENTS.md`/`DOMAIN_MODEL.md`에 명시적 규칙은 아니었으나 사용자가 이번 작업에서 직접 확정했다(`DOMAIN_MODEL.md` §4 "수업 ── 기준 ──> 출결 ── 대상 ──> 학생" 관계도와 일치).

## API Changes

- `POST /api/v1/lessons/{lessonId}/attendances` — 201, `{ "studentId": number, "status": "PRESENT" | "LATE" | "ABSENT" | "EXCUSED" }` → `AttendanceResponse`
- `GET /api/v1/lessons/{lessonId}/attendances` — 200, `AttendanceResponse[]`
- `GET /api/v1/attendances` — 200, `AttendanceResponse[]`(전체 목록, 관리자/선생님)
- `GET /api/v1/attendances/{id}` — 200 / 404
- `PATCH /api/v1/attendances/{id}` — 200, `{ "status": "PRESENT" | "LATE" | "ABSENT" | "EXCUSED" }`
- `AttendanceResponse`: `{ "id": number, "lessonId": number, "studentId": number, "status": "PRESENT" | "LATE" | "ABSENT" | "EXCUSED" }`

## Domain Impact

`DOMAIN_MODEL.md` 3.5 출결(Attendance)의 `Attendance` Aggregate를 신규 도입한다(`ADR-007`). `Attendance`는 `Lesson`, `Student` Aggregate를 각각 `lessonId`, `studentId` 값으로만 참조한다.

## Database Impact

`V4__create_attendances_table.sql` — `attendances` 테이블 신규 생성(위 Scope 참고).

## Exception and Error Handling

- 존재하지 않는 수업/학생/출결 id → 404(`AttendanceNotFoundException`, 수업/학생은 `LessonNotFoundException`/`StudentNotFoundException` 재사용).
- 배정되지 않은 학생에 대한 출결 등록, 중복 `(lessonId, studentId)` 등록 → 400.
- 입력값 검증 실패(상태값 누락 등) → 400(Spring 기본 처리).

## Test Scenarios

- `AttendanceTest`(도메인 단위): 등록, 상태 변경.
- `AttendanceApiTests`(MockMvc + Testcontainers): 등록·조회·수정 플로우, 배정되지 않은 학생 등록 시 400, 중복 등록 시 400, 존재하지 않는 id 404, TEACHER 외 역할의 등록/수정 403, ADMIN/TEACHER 외 역할의 조회 403, 미인증 401.

## Acceptance Criteria

- 위 API가 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `AttendanceNotFoundException`이 `AggregateNotFoundException`을 상속해 별도 `ExceptionHandler` 없이 404가 반환된다.
- `ARCHITECTURE.md` §8 Deferred Decision #1이 `ADR-007`로 완전히 해소되어 있음을 확인한다(이미 문서 반영 완료).

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- **학생/학부모 접근**: User-Student 연결, 학부모-학생 연결 방식이 확정되면 "본인/자녀 출결 조회" API를 별도 작업으로 추가해야 한다.

다음 항목은 사용자가 이번 작업 시작 시 직접 확정했다(더 이상 Open Question 아님):

- 배정 검증 규칙(배정된 학생만 출결 등록 가능) — 위 Business Rules 참고.
- 선생님의 조회 범위(담당 스코핑 없이 전체 조회 허용) — 위 Users and Permissions 참고.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`
