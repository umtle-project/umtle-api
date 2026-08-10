# TASK-005: 반/수업(Class/Lesson) 관리 도메인 구현

## Status

Ready

## Purpose

`ADR-006`의 결정에 따라 Class(반)와 Lesson(수업) Aggregate를 구현한다. 코드에서는 Kotlin/Java `Class` 타입과의 혼동을 피하기 위해 반 Aggregate를 `Classroom`으로 명명한다. `DOMAIN_MODEL.md`가 Student 다음 순서로 참조하는 도메인이며, 향후 일정/출결/숙제/학습 기록 도메인이 참조할 `classId`/`lessonId`의 기반이 된다.

## Background

`TASK-002`(Student), `TASK-003`(User + 인증/인가)가 완료되어 있고, `ARCHITECTURE.md` §8 Deferred Decision #1(Aggregate 경계)이 `ADR-006`으로 부분 해소되었다 — Class/Lesson은 별도 Aggregate, 반-학생/반-선생님 배정은 다대다이며 코드의 `Classroom`이 소유하는 순수 id 값 컬렉션으로 관리, Lesson 상태값은 `SCHEDULED`/`COMPLETED`/`CANCELLED`.

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.2 반 및 수업 관리
- `docs/DOMAIN_MODEL.md` 3.3 반/수업(Class/Lesson)
- `docs/USER_ROLES.md` 3.2 반 및 수업 관리
- `docs/adr/ADR-002-tsid-as-identifier.md`(ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md`(공통 404 처리)
- `docs/adr/ADR-005-query-strategy.md`(연관관계 금지, QueryDSL)
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md`(Aggregate 경계, 이 작업이 실제 적용 사례)

## Users and Permissions

`USER_ROLES.md` 3.2를 그대로 적용한다:

| 역할 | 권한 |
|------|------|
| 관리자 | 반 생성, 조회, 수정, 비활성화 / 반에 학생·선생님 배정·해제 / 수업 생성, 조회, 수정, 취소·완료 처리 |
| 선생님 | `USER_ROLES.md`에 서술 없음 → **기본 거부**. 임의로 조회 권한을 부여하지 않는다. |
| 학생 | 서술 없음 → **기본 거부**. |
| 학부모 | 서술 없음 → **기본 거부**. |

`TASK-002`(Student)의 "선생님 조회 권한"과 달리, 반/수업에는 선생님 조회 권한조차 `REQUIREMENTS.md`/`USER_ROLES.md`에 서술이 없다 — 이번 작업은 관리자 전용으로 구현하고, 선생님 조회 권한이 필요한지는 별도로 확인한다(아래 Open Questions).

## Preconditions

`TASK-002`(Student), `TASK-003`(User + 인증/인가), `ADR-006`(Accepted)이 존재해야 한다.

## Scope

### Class/Classroom 도메인

- `classroom/domain/Classroom.kt` — `Student.kt`와 동일한 패턴(private 생성자 + `register(name)` / `reconstitute(...)` companion factory). TSID `Long` id, `@GeneratedValue` 없이 factory에서 직접 할당(`ADR-002`).
- 필드: `id`, `name`, `status`(`ACTIVE`/`INACTIVE`), `studentIds: Set<Long>`, `teacherIds: Set<Long>`.
- 도메인 메서드: `rename(newName)`, `deactivate()`, `assignStudent(studentId)`/`unassignStudent(studentId)`, `assignTeacher(teacherId)`/`unassignTeacher(teacherId)`.
- `classroom/domain/ClassroomStatus.kt`, `classroom/domain/ClassroomNotFoundException.kt`(`common.domain.AggregateNotFoundException` 상속, `ADR-003` 공통 패턴), `classroom/domain/ClassroomRepository.kt`(포트).
- `classroom/infrastructure/ClassroomJpaEntity.kt` — `BaseEntity` 상속. 배정은 `ClassStudentJpaEntity(id, classId, studentId)`, `ClassTeacherJpaEntity(id, classId, teacherId)` 단순 row 엔티티로 저장한다. JPA `@ElementCollection`, `@JoinColumn`, 복합 PK, 물리 FK는 사용하지 않는다.
- `classroom/infrastructure/ClassroomJpaRepository.kt`, `ClassroomRepositoryAdapter.kt`.
- `classroom/application/ClassroomService.kt` — 등록/조회/목록/수정/비활성화/학생·선생님 배정·해제. 배정 시 대상 id가 실제 존재하는지 각각 `StudentRepository`(학생), `UserRepository`(선생님, `TEACHER` 역할 보유 여부 확인)로 검증 — JPA 연관관계가 아닌 Application Service 레벨의 ID 조회(`ARCHITECTURE.md` §6.1).
- `classroom/presentation/ClassroomController.kt`, `ClassroomDtos.kt`. 별도 `ExceptionHandler`는 만들지 않는다 — `common.presentation.GlobalExceptionHandler`가 `ClassroomNotFoundException`을 자동으로 404로 처리한다(`ADR-003`).

### Lesson 도메인

- `lesson/domain/Lesson.kt` — 필드: `id`, `classId: Long`(ID 참조, `Classroom` 객체 아님), `status`(`SCHEDULED`/`COMPLETED`/`CANCELLED`).
- 도메인 메서드: `cancel()`, `complete()` — `SCHEDULED` 상태에서만 전이 가능(그 외 상태에서 호출 시 예외).
- `lesson/domain/LessonStatus.kt`, `lesson/domain/LessonNotFoundException.kt`(`AggregateNotFoundException` 상속), `lesson/domain/LessonRepository.kt`.
- `lesson/infrastructure/LessonJpaEntity.kt`(`classId`는 일반 `Long` 컬럼, 연관관계 아님), `LessonJpaRepository.kt`, `LessonRepositoryAdapter.kt`.
- `lesson/application/LessonService.kt` — 등록 시 `classId`가 실제 존재하는 `Classroom`인지 `ClassroomRepository`로 검증. 조회/목록/수정/취소/완료.
- `lesson/presentation/LessonController.kt`, `LessonDtos.kt`.

### 데이터베이스

- `V3__create_classes_and_lessons_tables.sql`:

```sql
CREATE TABLE classes (
    id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE class_students (
    id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_students_class_student UNIQUE (class_id, student_id)
);

CREATE TABLE class_teachers (
    id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_class_teachers_class_teacher UNIQUE (class_id, teacher_id)
);

CREATE TABLE lessons (
    id BIGINT NOT NULL,
    class_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
);
```

물리 외래 키는 두지 않는다 — Aggregate 간 참조는 물론 배정 보조 테이블도 순수 id 값만 저장하고, 참조 대상 존재 여부는 Application Service에서 검증한다(`ARCHITECTURE.md` §6.1, ID 참조 원칙).

### 인가

- `SecurityConfig`에 `/api/v1/classes/**`, `/api/v1/lessons/**`를 `hasRole("ADMIN")`으로 추가 — 위 Users and Permissions 표에 따라 다른 역할은 전부 거부.

## Out of Scope

- Attendance/Homework/LearningRecord/Schedule 연동 — 해당 도메인이 아직 구현되지 않았다(`ADR-006`이 이 부분의 Aggregate 종속 관계를 계속 보류).
- 배정(assign)에 날짜, 승인 상태 등 부가 속성 — `ADR-006`에서 근거 부족으로 보류.
- "담당 학생"/"담당 반" 스코핑 — `USER_ROLES.md` §4 미정 항목.
- 선생님/학생/학부모의 반/수업 조회 권한 — `USER_ROLES.md`에 서술이 없어 이번에도 부여하지 않는다.
- `name` 외 반의 추가 필드(설명, 정원 등) — 필요해지면 별도 작업으로 추가.

## Functional Scenarios

- 관리자가 이름을 입력해 반을 등록하면 상태 `ACTIVE`로 생성된다.
- 관리자가 반에 학생/선생님을 배정하거나 해제할 수 있다. 존재하지 않는 학생/선생님 id로 배정하면 404를 반환한다.
- 관리자가 반을 비활성화하면 상태가 `INACTIVE`로 바뀐다.
- 관리자가 특정 반에 속한 수업을 등록하면 상태 `SCHEDULED`로 생성된다. 존재하지 않는 `classId`로 등록하면 404를 반환한다.
- 관리자가 수업을 취소하거나 완료 처리할 수 있다. 이미 `CANCELLED`/`COMPLETED`인 수업을 다시 전이하려 하면 오류를 반환한다.
- 존재하지 않는 반/수업 id를 조회/수정하면 404를 반환한다.
- 인증되지 않았거나 `ADMIN`이 아닌 사용자가 반/수업 API에 접근하면 401/403을 반환한다.

## Business Rules

- 반 이름은 공백일 수 없고 100자를 초과할 수 없다(`Student.register`와 동일한 검증 패턴).
- 같은 학생/선생님을 중복 배정해도 오류 없이 멱등하게 처리한다(`Set` 특성).
- Lesson은 `SCHEDULED` 상태에서만 `cancel()`/`complete()`가 가능하다 — 그 외 상태에서 호출하면 예외를 던진다(구현 시 가정, `REQUIREMENTS.md`/`DOMAIN_MODEL.md`에 명시적 규칙 없음).

## API Changes

- `POST /api/v1/classes` — 201, `{ "name": string }` → `ClassroomResponse`
- `GET /api/v1/classes` / `GET /api/v1/classes/{id}` — 200 / 404
- `PATCH /api/v1/classes/{id}` — 200, `{ "name": string }`
- `POST /api/v1/classes/{id}/deactivate` — 200
- `POST /api/v1/classes/{id}/students` — 200, `{ "studentId": number }` (배정) / `DELETE /api/v1/classes/{id}/students/{studentId}`(해제)
- `POST /api/v1/classes/{id}/teachers` — 200, `{ "teacherId": number }` / `DELETE /api/v1/classes/{id}/teachers/{teacherId}`
- `ClassroomResponse`: `{ "id": number, "name": string, "status": "ACTIVE" | "INACTIVE", "studentIds": number[], "teacherIds": number[] }`
- `POST /api/v1/lessons` — 201, `{ "classId": number }` → `LessonResponse`
- `GET /api/v1/lessons` / `GET /api/v1/lessons/{id}` — 200 / 404
- `POST /api/v1/lessons/{id}/cancel`, `POST /api/v1/lessons/{id}/complete` — 200
- `LessonResponse`: `{ "id": number, "classId": number, "status": "SCHEDULED" | "COMPLETED" | "CANCELLED" }`

## Domain Impact

`DOMAIN_MODEL.md` 3.3 반/수업(Class/Lesson)의 `Class`, `Lesson` 두 Aggregate를 신규 도입한다(`ADR-006`). 코드에서는 Class를 `Classroom`으로 구현한다. `Classroom`은 `Student`, `User` Aggregate를 id로만 참조하고, `Lesson`은 `Classroom`을 id로만 참조한다.

## Database Impact

`V3__create_classes_and_lessons_tables.sql` — `classes`, `class_students`, `class_teachers`, `lessons` 테이블 신규 생성(위 Scope 참고).

## Exception and Error Handling

- 존재하지 않는 반/수업/배정 대상(학생/선생님) → 404(`ClassroomNotFoundException`/`LessonNotFoundException`, 학생/선생님은 `StudentNotFoundException`/`UserNotFoundException` 재사용).
- Lesson의 잘못된 상태 전이(이미 종료된 수업 재취소 등) → 400.
- 입력값 검증 실패(빈 이름 등) → 400(Spring 기본 처리).

## Test Scenarios

- `ClassroomTest`(도메인 단위): 등록 검증, 학생/선생님 배정·해제, 중복 배정 멱등성, 비활성화.
- `LessonTest`: 등록, 취소/완료 전이, 잘못된 상태에서 전이 시도 시 예외.
- `ClassroomApiTests`/`LessonApiTests`(MockMvc + Testcontainers): 전체 CRUD 플로우, 존재하지 않는 id 404, ADMIN 외 역할 403, 미인증 401.

## Acceptance Criteria

- 위 API가 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `ClassroomNotFoundException`/`LessonNotFoundException`이 `AggregateNotFoundException`을 상속해 별도 `ExceptionHandler` 없이 404가 반환된다.
- `ARCHITECTURE.md` §8 Deferred Decision #1이 `ADR-006`으로 부분 해소되어 있다(Attendance/Homework/LearningRecord 부분은 계속 보류로 남아 있음을 확인).

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- **배정 대상의 의미**: "학생"은 `Student`(TASK-002) Aggregate의 id, "선생님"은 `User`(TASK-003) Aggregate 중 `TEACHER` 역할을 가진 사용자의 id로 가정했다 — `DOMAIN_MODEL.md` §3.2("학생은 반, 수업 등과 연결되는 중심 엔티티")에 근거하지만, Student와 User가 서로 연결되어 있지 않은 현재 구조상 실제 의도와 맞는지 사람 확인이 필요하다.
- Lesson의 상태 전이 규칙(`SCHEDULED`에서만 취소/완료 가능)은 `REQUIREMENTS.md`/`DOMAIN_MODEL.md`에 명시된 규칙이 아니라 구현 시 채택한 가정이다 — 확인 권장.
- 선생님의 반/수업 조회 권한 여부 — `USER_ROLES.md`에 서술이 없어 이번 작업은 기본 거부로 구현하지만, 실제로 필요하다면 `USER_ROLES.md`부터 갱신 후 별도 작업으로 추가한다.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md`
