# TASK-018: 역할별 사용자 목록 조회 API (선생님 목록 조회)

## Status

Ready for implementation

## Purpose

관리자가 반에 담임 선생님을 배정할 때(`POST /api/v1/classes/{id}/teacher`) `teacherId`를 직접 입력해야 하는 불편을 해소한다. 승인된 `TEACHER` 역할 사용자 목록을 조회할 수 있는 API를 추가해, 프론트엔드에서 id 직접 입력 대신 선택 UI(드롭다운/검색 목록)를 구성할 수 있게 한다.

## Background

`TASK-017`(반-선생님 배정 1:N 정정) 작업 중, 관리자가 `Classroom`에 담임을 배정하려면 `teacherId`(숫자)를 알고 있어야 한다는 점이 지적되었다. 현재 `UserController`에는 전체 사용자를 역할로 필터링해 조회하는 API가 없다 — `GET /api/v1/users/pending?role=...`은 **승인 대기 중**인 사용자만 대상이고, `GET /api/v1/users/{id}`는 id를 이미 알아야 하는 단건 조회다. `UserRepository`에도 목록 조회 메서드가 없고 `existsByRole`/`existsByIdAndRole`처럼 존재 여부 확인용 메서드만 있다.

## Related Documents and Requirement IDs

- `docs/tasks/TASK-017-teacher-classroom-scope.md` — 이 작업의 계기가 된 담임 배정 플로우
- `docs/USER_ROLES.md` §2 — 역할 정의(ADMIN/TEACHER/STUDENT/PARENT)

## Users and Permissions

| 역할 | 변경 사항 |
|------|-----------|
| 관리자 | 신규 API로 승인된 사용자를 역할로 필터링해 조회 가능. |
| 선생님/학생/학부모 | 변경 없음 — 이 API는 관리자 전용(`SecurityConfig`의 기존 `/api/v1/users/**` → `hasRole("ADMIN")` 규칙을 그대로 적용받음, 규칙 추가/변경 없음). |

## Preconditions

없음 — 기존 `User`/`UserRole`/`UserStatus` 도메인 모델과 `UserRepositoryAdapter`의 QueryDSL 조회 구조(`findUsers { ... }`)를 그대로 활용한다.

## Scope

### 1. 도메인/리포지토리 계층

- `src/main/kotlin/com/umtle/umtleapi/user/domain/UserRepository.kt`
  - `findAllByRoleAndStatus(role: UserRole, status: UserStatus): List<User>` 메서드 추가.
- `src/main/kotlin/com/umtle/umtleapi/user/infrastructure/UserRepositoryAdapter.kt`
  - 기존 `findUsers { applyWhere }` private 헬퍼를 재사용해 구현한다: `userRole.role.eq(role)`과 `user.status.eq(status)` 조건으로 QueryDSL 조회.
  - 새 JPA 레벨 메서드가 필요하면 기존 컨벤션(`UserRoleJpaRepository`의 `existsByUserIdAndRole` 등)에 맞춰 추가한다.

### 2. 애플리케이션 계층 (`UserService`)

- `listUsersByRole(role: UserRole): List<User>` 추가.
  - 항상 `UserStatus.ACTIVE` 상태인 사용자만 반환한다(대기/비활성 사용자는 담임 배정 대상이 아니므로 제외 — 대기 중인 사용자는 기존 `/pending` API로 별도 조회).
  - 역할/상태 외 추가 인가 로직 없음 — 컨트롤러 레벨에서 이미 `ADMIN` 전용으로 막혀 있음.

### 3. API 계층 (`UserController`/`UserDtos`)

- `GET /api/v1/users?role={role}` 신규 추가.
  - `role`: 필수 쿼리 파라미터, `UserRole` enum(`ADMIN`/`TEACHER`/`STUDENT`/`PARENT`) — 이번 목적은 `TEACHER`이지만, 재사용 가능하도록 특정 역할에 종속시키지 않고 일반 역할 필터로 구현한다.
  - 응답: 기존 `UserResponse`(변경 없음)를 그대로 사용해 `List<UserResponse>` 반환 — 새 DTO를 만들지 않는다.
  - 신규 보안 규칙 불필요 — `SecurityConfig`의 `authorize("/api/v1/users/**", hasRole("ADMIN"))`이 이미 이 경로를 포함한다.

## Out of Scope

- 프론트엔드(umtle-web) 변경 — 이 API를 소비하는 UI 작업은 별도로 진행한다.
- 이름/loginId 기반 검색이나 페이지네이션 — 현재 선생님 수 규모를 고려해 전체 목록 반환으로 충분하다고 가정한다. 필요해지면 별도 작업으로 확장한다.
- `TEACHER` 외 다른 역할(`STUDENT`/`PARENT`)에 대한 별도 목적의 목록 API 설계 — 이번엔 범용 필터만 추가하고, 각 역할별 전용 UX(예: 학생 검색은 이미 `GET /api/v1/students/search`로 존재)는 다루지 않는다.
- `PENDING`/`INACTIVE` 상태 사용자를 포함하는 옵션 — 필요해지면 별도 쿼리 파라미터로 확장한다.

## Functional Scenarios

- 관리자가 `GET /api/v1/users?role=TEACHER`를 호출하면 승인 완료(`ACTIVE`)된 `TEACHER` 역할 사용자 목록을 받는다.
- 승인 대기 중이거나 비활성화된 선생님은 목록에 포함되지 않는다.
- 관리자가 아닌 사용자가 호출하면 403을 반환한다(기존 `SecurityConfig` 규칙, 변경 없음).
- 해당 역할의 승인된 사용자가 없으면 빈 배열을 반환한다(에러 아님).

## Business Rules

- 목록 조회는 항상 `status = ACTIVE`인 사용자만 대상으로 한다.

## API Changes

- `GET /api/v1/users?role={UserRole}` — 200, `List<UserResponse>` (신규)

## Domain Impact

없음 — 기존 `User`/`UserRole`/`UserStatus` 도메인 모델 변경 없이 조회 메서드만 추가한다.

## Database Impact

없음.

## Exception and Error Handling

- `role` 쿼리 파라미터 누락/유효하지 않은 값 → 400(Spring 기본 바인딩 예외 처리, 기존 컨벤션 재사용).
- 관리자가 아닌 사용자의 호출 → 403(`SecurityConfig` 필터, 변경 없음).

## Test Scenarios

- `UserApiTests`(또는 관련 API 테스트 클래스)에 다음 케이스 추가:
  - 관리자가 `role=TEACHER`로 조회 시 승인된 선생님만 반환.
  - 승인 대기/비활성 선생님은 결과에서 제외.
  - 관리자가 아닌 사용자(또는 미인증)가 호출 시 403/401.
  - 해당 역할의 승인된 사용자가 없을 때 빈 배열 반환.

## Acceptance Criteria

- 위 API가 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- 기존 `UserController`/`UserService` 관련 테스트가 이번 변경으로 깨지지 않는다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준.

## Open Questions

없음.

## Related ADRs

없음 — 기존 역할/조회 패턴을 그대로 확장하는 작업으로 별도 아키텍처 결정이 필요하지 않다.
