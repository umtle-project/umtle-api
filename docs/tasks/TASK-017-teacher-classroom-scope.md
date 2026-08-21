# TASK-017: 반-선생님 배정 1:N 정정 및 선생님 쓰기 권한의 담당 학생 제한

## Status

Ready for implementation — `docs/adr/ADR-014-teacher-classroom-scope.md` 결정을 구현한다.

## Purpose

`Classroom.teacherIds`(다대다)를 실제 운영 규칙인 반-선생님 1:N에 맞게 `teacherId`(단일, nullable)로 정정하고, 선생님이 `Attendance`/`Homework`/`LearningRecord`를 쓸 때 담당 학생(자신이 담임인 반에 속한 학생)에게만 쓸 수 있도록 제한한다.

## Background

`TASK-006`(출결), `TASK-007`(숙제), `TASK-013`, `TASK-014`(학습 기록), `TASK-015`(일정)는 모두 "담당 학생/담당 반 배정 기준 미정"을 Open Question으로 남겼다. `ADR-014`가 이를 해소했다: 반-선생님은 1:N이며, `Attendance`/`Homework`/`LearningRecord`는 모두 `studentId`를 필수로 가지므로(`ADR-007`) "담당 학생" 하나의 기준으로 세 도메인의 쓰기 스코핑을 통일할 수 있다. 자세한 배경과 대안 검토는 `ADR-014`를 따른다.

## Related Documents and Requirement IDs

- `docs/adr/ADR-014-teacher-classroom-scope.md` — 이 작업이 구현하는 결정
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md` — 반-선생님 배정 부분이 이 작업으로 amend됨
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md` — 세 도메인의 `studentId` 필수 참조 구조
- `docs/USER_ROLES.md` §4 — "담당 학생/담당 반 배정 기준" 항목 해소 대상
- `docs/tasks/TASK-006-attendance-management.md`, `TASK-007-homework-management.md`, `TASK-014-learning-record-management.md`

## Users and Permissions

| 역할 | 변경 사항 |
|------|-----------|
| 관리자 | 반-선생님 배정 API(`teacherId` 단일화) 사용 방식만 변경. `Attendance`/`Homework`/`LearningRecord` 쓰기 권한은 이전부터 없었고 계속 없음. |
| 선생님 | 조회(`GET`)는 변경 없음(전체 대상). `Attendance`/`Homework`/`LearningRecord`의 생성·수정·삭제는 대상 `studentId`가 본인 담당 학생일 때만 허용 — 아니면 403. |
| 학생/학부모 | 변경 없음. |

## Preconditions

`ADR-014`가 Accepted 상태여야 한다. `TASK-006`, `TASK-007`, `TASK-014`가 구현되어 있어야 한다(모두 완료됨).

## Scope

### 1. `Classroom` 도메인 모델 변경

- `src/main/kotlin/com/umtle/umtleapi/classroom/domain/Classroom.kt`
  - `teacherIds: Set<Long>` → `teacherId: Long?`(nullable, 단일)로 변경.
  - `assignTeacher(teacherId: Long)`: 기존 `teacherId`가 있으면 교체(덮어쓰기)한다 — 별도 예외 없이 새 값으로 대체.
  - `unassignTeacher()`: `teacherId`를 `null`로 설정한다. 인자 없음(단일 값이므로 어떤 선생님을 해제할지 지정할 필요가 없다).
- `ClassroomService.assignTeacher`/`unassignTeacher` 시그니처를 위 도메인 변경에 맞춘다. `assignTeacher`의 "대상 사용자가 `TEACHER` 역할인지" 검증(`InvalidTeacherAssignmentException`)은 그대로 유지한다.

### 2. 영속성 계층 변경

- `V9__...` 이후 다음 마이그레이션 번호로 `V10__replace_class_teachers_with_teacher_id.sql` 작성(정확한 다음 번호는 구현 시점의 `src/main/resources/db/migration/` 최신 파일 기준으로 확인):
  - `classes` 테이블에 `teacher_id BIGINT NULL` 컬럼 추가.
  - 기존 `class_teachers`의 반별 첫 번째(또는 유일한) `teacher_id`를 `classes.teacher_id`로 이행하는 데이터 마이그레이션 SQL 포함. **반 하나에 선생님이 2명 이상 배정된 기존 데이터가 있는지 먼저 확인**하고, 있다면 어떤 것을 남길지(가장 최근 배정 등) 마이그레이션 SQL에 명시적으로 반영한다.
  - `class_teachers` 테이블 삭제.
- `ClassTeacherJpaEntity`, `ClassTeacherJpaRepository` 삭제.
- `ClassroomJpaEntity`에 `teacherId: Long?` 컬럼 추가, `ClassroomRepositoryAdapter`의 `syncTeachers`/`findTeacherIdsByClassId` 등 다대다 동기화 로직을 단일 컬럼 읽기/쓰기로 교체.

### 3. API 변경 (`ClassroomController`/`ClassroomDtos`)

- 기존 `POST /api/v1/classes/{id}/teachers` (`AssignTeacherRequest`), `DELETE /api/v1/classes/{id}/teachers/{teacherId}`를 아래로 교체한다(Breaking Change — 자세한 내용은 API Changes 참고).
- `ClassroomResponse.teacherIds: Set<Long>` → `teacherId: Long?`.

### 4. 선생님 쓰기 권한 스코핑 (`Attendance`/`Homework`/`LearningRecord`)

- `ClassroomRepository`에 담당 학생 판별용 메서드를 추가한다 — 예: `isStudentInTeacherScope(teacherId: Long, studentId: Long): Boolean`(정확한 메서드명은 구현 시 기존 네이밍 컨벤션에 맞춘다). `teacherId`로 배정된 모든 `Classroom`의 `studentIds`에 해당 `studentId`가 포함되는지로 판별한다.
- `AttendanceService.record`/`updateStatus`, `HomeworkService.assign`/`update`/`delete`, `LearningRecordService.record`/`update` 각각에서, 현재 로그인한 사용자가 `TEACHER`이면 대상 레코드의 `studentId`가 본인 담당 학생인지 검증한다. 아니면 새 예외를 던져 403으로 응답한다(Exception and Error Handling 참고).
- 현재 로그인 사용자의 id를 얻는 방법은 기존 컨벤션(`SecurityContextHolder` 기반 loginId → `User` 조회, `UserController`/`AuthController` 패턴 참고)을 따른다. Application Service가 "현재 사용자"를 알아야 하므로, Controller에서 로그인 사용자 id를 조회해 Service 메서드 인자로 전달하는 방식을 유지한다(기존 프로젝트에 인증 정보를 얻는 별도 argument resolver가 없다면 새로 만들지 않고 기존 패턴을 그대로 재사용한다).
- 관리자는 이 세 도메인에 대한 쓰기 API 권한이 `SecurityConfig`에서 이미 없으므로(`hasRole("TEACHER")` 전용), 이번 스코핑 로직은 `TEACHER`에게만 적용하면 된다 — 관리자 우회 경로를 새로 만들지 않는다.

## Out of Scope

- 선생님의 **조회(GET)** 권한 제한 — 계속 전체 허용(`ADR-014` Decision 3).
- 학생/학부모의 "본인/자녀 관련" 조회 스코핑 — `TASK-015` Open Questions 그대로 별도 작업으로 남긴다.
- `Schedule` 도메인 — 선생님은 이미 쓰기 권한이 전혀 없어(`TASK-015`) 이 작업의 대상이 아니다.
- 관리자에게 `Attendance`/`Homework`/`LearningRecord` 쓰기 권한을 새로 부여하는 것 — 현재 정책 유지.
- 반 미배정 학생에 대한 관리자 우회 쓰기 경로 — `ADR-014` Risks에 남긴 대로 이번에는 다루지 않는다.
- "담당 수업" 개념을 `Classroom`/`Lesson` 자체의 관리 권한(생성/조회/수정/비활성화)에 확대 적용하는 것 — 계속 관리자 전용.

## Functional Scenarios

- 관리자가 반에 선생님을 배정하면(`teacherId` 지정) 반영되고, 이미 배정된 선생님이 있으면 교체된다.
- 관리자가 반의 선생님 배정을 해제할 수 있다.
- `TEACHER` 역할이 아닌 사용자를 배정하려 하면 400을 반환한다(`InvalidTeacherAssignmentException`, 기존 유지).
- 선생님이 본인 담당 학생(자신이 담임인 반에 속한 학생)에 대해 출결/숙제/학습 기록을 생성·수정·삭제하면 정상 처리된다.
- 선생님이 담당하지 않는 학생에 대해 출결/숙제/학습 기록을 생성·수정·삭제하려 하면 403을 반환한다.
- 어떤 반의 담임도 아닌 선생님이 출결/숙제/학습 기록을 쓰려 하면(담당 학생이 없음) 항상 403을 반환한다.
- 선생님의 조회(전체 목록, 학생별/수업별 조회)는 담당 여부와 무관하게 기존처럼 동작한다.

## Business Rules

- 반(`Classroom`) 하나에는 선생님이 최대 1명 배정된다.
- 선생님의 담당 학생 = 그 선생님이 담임인 모든 반의 `studentIds` 합집합.
- `Attendance`/`Homework`/`LearningRecord` 쓰기 시 선생님 본인이 담당하는 학생인지 항상 검증한다(생성은 요청의 `studentId`, 수정/삭제는 기존 레코드의 `studentId` 기준).

## API Changes

- `POST /api/v1/classes/{id}/teacher` — 200, `{ "teacherId": number }` → `ClassroomResponse`(기존 배정이 있으면 교체)
- `DELETE /api/v1/classes/{id}/teacher` — 204 (경로에 `teacherId` 없음 — 단일 값이므로 대상이 항상 명확)
- `ClassroomResponse`: `teacherIds: number[]` → `teacherId: number | null`
- 기존 `POST /api/v1/classes/{id}/teachers`, `DELETE /api/v1/classes/{id}/teachers/{teacherId}`는 제거한다(Breaking Change — 프론트엔드 연동 시 반영 필요).
- `Attendance`/`Homework`/`LearningRecord`의 기존 쓰기 API 경로·요청/응답 스키마는 변경 없음 — 인가 결과(403)만 추가된다.

## Domain Impact

- `DOMAIN_MODEL.md` §3.3: "반-학생, 반-선생님 배정은 다대다이며..." 문장을 "반-학생 배정은 다대다, 반-선생님 배정은 1:N이며..."로 갱신(`ADR-014` 반영).
- `USER_ROLES.md` §4: "담당 학생/담당 반의 배정 기준" 항목을 해소된 것으로 표시하고 관련 각주 추가.

## Database Impact

- `V10__replace_class_teachers_with_teacher_id.sql`(정확한 번호는 구현 시점 기준): `classes.teacher_id BIGINT NULL` 추가, 데이터 이행, `class_teachers` 테이블 삭제.

## Exception and Error Handling

- `TEACHER` 역할이 아닌 사용자를 반 담임으로 배정 시도 → 400(`InvalidTeacherAssignmentException`, 기존 재사용).
- 존재하지 않는 반/사용자 id로 배정 시도 → 404(기존 `ClassroomNotFoundException`/`UserNotFoundException` 재사용).
- 선생님이 담당하지 않는 학생의 출결/숙제/학습 기록을 쓰려는 시도 → 403(새 예외, 도메인별로 각각 정의하거나 공통 예외 하나로 통일 — 구현 시 기존 프로젝트의 예외 네이밍 컨벤션에 맞춰 결정. 예: `UnassignedTeacherWriteException`).
- 관리자/미인증 사용자가 위 세 도메인 쓰기 API를 호출 → 기존과 동일하게 403/401(`SecurityConfig` 역할 기반 필터가 먼저 차단, 변경 없음).

## Test Scenarios

- `ClassroomTest`(도메인 단위): 선생님 배정, 재배정 시 교체, 해제.
- `ClassroomApiTests`: 선생님 배정/해제 API(신규 경로), `TEACHER` 아닌 사용자 배정 시 400.
- `AttendanceApiTests`/`HomeworkApiTests`/`LearningRecordApiTests`에 다음 케이스 추가:
  - 담당 학생에 대한 생성/수정/삭제 → 정상 처리.
  - 담당 아닌 학생에 대한 생성/수정/삭제 → 403.
  - 담당 반이 없는 선생님의 생성 시도 → 403.
  - 기존 조회(GET) 테스트는 담당 여부와 무관하게 그대로 통과해야 한다(회귀 확인).

## Acceptance Criteria

- 위 API와 스코핑 규칙이 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- 기존 `Attendance`/`Homework`/`LearningRecord`/`Classroom` 관련 테스트가 이번 변경으로 깨지지 않고, 필요한 부분은 담당 학생 데이터를 포함하도록 갱신된다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

- **반 미배정 학생/담당 반 없는 선생님**: `ADR-014` Risks에 남긴 대로, 반 배정이 누락된 학생은 어떤 선생님도 기록을 쓸 수 없다. 실제 운영에서 문제가 되면 관리자 우회 권한 부여 여부를 별도로 재검토한다.
- **선생님 조회 권한 제한**: 이번에는 다루지 않는다. 학생/학부모 스코핑과 함께 필요해지면 별도 작업으로 진행한다.
- **기존 데이터 정리**: `class_teachers`에 반 하나당 선생님이 2명 이상인 기존 데이터가 있는지 마이그레이션 작성 전에 반드시 확인한다.

## Related ADRs

- `docs/adr/ADR-014-teacher-classroom-scope.md`
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md`
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`
