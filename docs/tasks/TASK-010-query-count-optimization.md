# TASK-010: 불필요한 도메인 재구성 조회를 줄인다

## Status

In Review

## Purpose

SQL 로그 도입 이후 드러난 과도한 조회를 줄인다. 존재 확인이나 배정 여부 확인만 필요한 흐름에서 전체 Aggregate를 재구성하지 않도록 전용 조회 포트를 추가한다.

## Background

`TASK-009`로 SQL 로그가 보이기 시작한 뒤, 일부 요청에서 기대보다 많은 select가 실행되는 문제가 확인되었다. 주요 원인은 다음 두 가지였다.

- `User`와 `Classroom`은 역할/배정 id 컬렉션을 별도 테이블로 저장하므로, 전체 Aggregate 재구성 시 하위 테이블 조회가 추가로 필요하다.
- 일부 Application Service는 존재 확인이나 배정 여부 확인만 필요한데도 `findById()`로 전체 Aggregate를 가져왔다.

`ARCHITECTURE.md` §6.1은 JPA 연관관계 매핑을 금지하지만, ID 기반 명시 조인 또는 전용 exists 조회는 허용한다. 이 TASK는 JPA 연관관계 없이 불필요한 조회를 줄이는 최소 변경에 한정한다.

## Related Documents and Requirement IDs

- `docs/ARCHITECTURE.md` §6.1 JPA 연관관계 사용 금지
- `docs/ARCHITECTURE.md` §6.2 트랜잭션 관리
- `docs/adr/ADR-005-query-strategy.md` — ID 기반 QueryDSL/JPQL 조회 전략
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md` — Class 배정 id 컬렉션 관리
- `docs/adr/ADR-008-user-student-parent-connection.md` — User-Student/Parent 연결 방식
- `docs/tasks/TASK-009-request-tracing-and-logging.md` — SQL 로그를 통해 문제를 확인한 선행 작업

## Users and Permissions

사용자 권한 정책은 변경하지 않는다. 기존 Admin/Teacher 승인, 반 배정, 출결, 숙제 API의 권한과 응답 의미를 그대로 유지한다.

## Preconditions

- `TASK-009`의 SQL 로그 설정이 적용되어 있어 쿼리 흐름을 확인할 수 있어야 한다.
- `TASK-004`로 QueryDSL 기반 조회 인프라가 이미 존재한다.

## Scope

- `UserRepositoryAdapter`에서 `User`, `UserRole`, `ParentStudent`를 ID 기반 QueryDSL left join으로 조회해 `findById`, `findByLoginId`, `findPendingByRoles`의 N+1/중복 조회를 줄인다.
- `ClassroomRepository.findAll()`은 `class_students`, `class_teachers`를 `IN` 배치 조회로 가져와 목록 조회 N+1을 피한다.
- 존재 확인만 필요한 흐름에 전용 포트를 추가한다.
  - `ClassroomRepository.existsById`
  - `ClassroomRepository.existsStudentAssignment`
  - `StudentRepository.existsById`
  - `UserRepository.existsById`
  - `UserRepository.existsByIdAndRole`
- 다음 호출부에서 전체 Aggregate 로딩을 exists 조회로 대체한다.
  - `LessonService.registerLesson`
  - `AttendanceService.record`
  - `HomeworkService.assign`
  - `HomeworkService.findAllByStudentId`
  - `HomeworkService.validateLessonAssignment`
  - `ClassroomService.assignStudent`
  - `ClassroomService.assignTeacher`
- QueryDSL 전환 이후 사용하지 않는 Spring Data JPA 메서드를 제거한다.

## Out of Scope

- JPA 연관관계(`@ManyToOne`, `@OneToMany`, `@ManyToMany`) 도입.
- 물리 외래 키 도입.
- Redis/cache 도입.
- CQRS 또는 별도 read model 도입.
- `JpaRepository.save()`의 assigned id 처리로 인한 insert 전 select 최적화. 필요하면 별도 TASK에서 `Persistable` 또는 `EntityManager.persist/merge` 전략으로 다룬다.
- 페이지네이션, 정렬 정책 변경.
- API 응답 형식 변경. 단, 별도 수정으로 JSON Long 문자열 직렬화 정책이 적용되어 있을 수 있으나 이 TASK의 핵심 범위는 쿼리 수 최적화다.

## Functional Scenarios

1. 관리자가 수업을 생성할 때, 반 존재 확인을 위해 `Classroom` 전체와 학생/선생님 배정 목록을 로드하지 않는다.
2. 선생님이 출결을 기록할 때, 학생 존재와 반-학생 배정 여부를 전용 exists 조회로 검증한다.
3. 선생님이 수업 단위 숙제를 부여할 때, 학생 존재와 반-학생 배정 여부를 전용 exists 조회로 검증한다.
4. 관리자가 반에 선생님을 배정할 때, 대상 사용자의 전체 roles/parent links를 로드하지 않고 사용자 존재와 TEACHER role 여부만 검증한다.
5. 승인 대기 사용자 목록 조회는 필터 역할을 만족하는 사용자를 찾되, 응답에는 대상 사용자의 전체 roles와 child student ids가 유지된다.

## Business Rules

- 기존 권한과 도메인 규칙은 변경하지 않는다.
- 존재하지 않는 학생/반/사용자/수업에 대한 오류 응답은 기존과 동일하게 유지한다.
- 배정되지 않은 학생에 대한 출결/숙제 등록은 기존처럼 거부한다.
- 승인 대상 사용자 판별은 기존처럼 `PENDING` 상태와 역할 기준을 따른다.

## API Changes

없음. 기존 endpoint, status code, request/response 의미를 유지한다.

## Domain Impact

Domain 객체의 상태나 불변식은 변경하지 않는다. Repository 포트에 "전체 Aggregate 조회"가 아닌 "존재/배정 여부 확인" 메서드가 추가된다.

## Database Impact

스키마 변경 없음.

향후 parent-child link 중복 방어가 필요하면 `parent_student_links(parent_user_id, student_id)` unique 제약 추가를 별도 TASK로 다룬다.

## Exception and Error Handling

- 전용 exists 조회로 바뀌어도 기존 not-found 예외는 유지한다.
- 반-학생 배정 여부 확인 실패는 기존 `UnassignedAttendanceStudentException`, `UnassignedHomeworkStudentException` 흐름을 유지한다.
- 선생님 role이 없는 사용자를 반에 배정하려는 경우 기존 `InvalidTeacherAssignmentException`을 유지한다.

## Test Scenarios

- 기존 API 통합 테스트가 모두 통과해야 한다.
- 학생/학부모/선생님 가입 승인 테스트가 통과해야 한다.
- 출결/숙제 배정 검증 테스트가 통과해야 한다.
- 반 학생/선생님 배정 테스트가 통과해야 한다.
- `./gradlew check`가 통과해야 한다.

## Acceptance Criteria

- 존재 확인만 필요한 흐름에서 `findById()` 기반 전체 Aggregate 재구성을 사용하지 않는다.
- `Classroom` 목록 조회에서 class별 학생/선생님 배정 조회 N+1이 발생하지 않는다.
- `User` 조회에서 role/parent link 조회 N+1이 발생하지 않는다.
- 기존 API 동작과 에러 응답 의미가 변경되지 않는다.
- `./gradlew check`가 통과한다.

## Definition of Done

- `docs/DEVELOPMENT.md` § Definition of Done 기준을 만족한다.
- 관련 테스트가 통과한다.
- 구현 후 별도 Review Agent 또는 사람의 검토를 거친다.

## Open Questions

- `JpaRepository.save()`의 assigned id 처리로 발생할 수 있는 insert 전 select를 지금 최적화할지 여부는 미정이다. 현재는 별도 TASK 후보로 남긴다.
- `parent_student_links(parent_user_id, student_id)` unique 제약을 추가할지 여부는 미정이다. 현재 정상 경로에서는 도메인 Set과 sync diff로 중복을 방지한다.

## Related ADRs

- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md`
- `docs/adr/ADR-008-user-student-parent-connection.md`
