# TASK-008: 사용자 자가 회원가입 및 역할별 승인

## Status

In Review — implementation complete, independent Review Agent 검토 필요

## Purpose

선생님, 학생, 학부모가 관리자 개입 없이 스스로 계정을 만들고, 역할에 맞는 승인 주체(관리자 또는 활성 선생님)의 승인을 받아 계정을 활성화할 수 있게 한다. 동시에 `ADR-008`이 결정만 해두고 구현되지 않았던 `User`-`Student` 연결 필드(`studentId`, `childStudentIds`)를 처음으로 구현한다.

## Background

현재 계정 생성은 인증된 `ADMIN`이 `POST /api/v1/users`로만 할 수 있다(`user/presentation/UserController.kt`, `config/SecurityConfig.kt`의 `/api/v1/users/**` → `hasRole("ADMIN")`). 자가 회원가입은 지금까지 요구사항 문서 어디에도 서술된 적이 없었다. 이 공백과, 역할별 승인 정책, `ADR-008` 연결 필드의 미구현 상태는 `ADR-009`에서 결정되었다. 이 TASK는 그 결정을 구현한다.

## Related Documents and Requirement IDs

- `docs/adr/ADR-009-user-signup-and-approval.md` — 이 TASK가 구현하는 결정
- `docs/adr/ADR-008-user-student-parent-connection.md` — 재사용하는 연결 필드 구조(`studentId`, `childStudentIds`)
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md` — 재사용하는 다대다 행 엔티티 패턴(`ClassStudentJpaEntity`)
- `docs/USER_ROLES.md` §4 — "담당 선생님/담당 반" 기준 미정, 이 TASK의 승인 범위와 직결
- `docs/DOMAIN_MODEL.md` §3.1 (사용자), §3.2 (학생)

## Users and Permissions

| 역할 | 권한 |
|------|------|
| 비인증(익명) | 회원가입 신청(`POST /api/v1/auth/signup`), 학생 검색(`GET /api/v1/students/search`) |
| 관리자 (Admin) | 대기 중인 선생님 계정 목록 조회·승인·거절 / 기존 `POST /api/v1/users`를 통한 계정 직접 생성(변경 없음) |
| 선생님 (Teacher) | `ACTIVE` 상태일 때만, 대기 중인 학생·학부모 계정 목록 조회·승인·거절 |
| 학생 (Student) | 이 TASK로 새로 부여되는 권한 없음 |
| 학부모 (Parent) | 이 TASK로 새로 부여되는 권한 없음 |

`ADR-009`에 따라 "담당 선생님" 제한 없이 활성 상태인 아무 선생님이나 학생/학부모 승인이 가능하다 — 이 기준이 확정되지 않았기 때문이며, `USER_ROLES.md` 4장이 갱신되면 재검토 대상이다.

## Preconditions

- `ADR-009`가 `Accepted` 상태로 전환되어 있어야 한다.
- 학생/학부모로 가입하려는 지원자가 연결하려는 `Student` 레코드가 이미 관리자에 의해 등록되어 있어야 한다(`Student.register`, 기존 TASK-002 기능, 변경 없음).

## Scope

- `User` 도메인에 `name`(표시 이름), `studentId`(선택적 1:1 학생 연결) 필드 추가.
- `User`에 `childStudentIds`(다대다 학부모-학생 연결) 필드 추가, `ADR-006` 패턴을 재사용하는 `ParentStudentJpaEntity`/`ParentStudentJpaRepository` 신설.
- `UserStatus`에 `PENDING` 추가.
- 공개 회원가입 API(`POST /api/v1/auth/signup`), 공개 학생 검색 API(`GET /api/v1/students/search`).
- 승인 대기 목록 조회, 승인, 거절 API(`GET /api/v1/users/pending`, `POST /api/v1/users/{id}/approve`, `POST /api/v1/users/{id}/reject`).
- `SecurityConfig.kt`에 위 신규 엔드포인트에 대한 인가 규칙 추가.
- Flyway 마이그레이션(`V6__...sql`)으로 위 스키마 변경 반영.

## Out of Scope

- 이미 활성화된 계정에 대해 관리자가 `studentId`/`childStudentIds`를 직접 생성·해제하는 전용 API(`ADR-008`이 관리자에게 이미 부여한 권한이지만, 이 TASK의 범위가 아니다 — 별도 TASK로 다룬다).
- "담당 선생님/담당 반" 기준에 따라 승인 권한을 좁히는 것(`USER_ROLES.md` 4장 미정).
- 이미 승인된 학부모 계정에 자녀를 추가로 연결하는 자가 서비스 흐름.
- 이메일/SMS 등을 통한 가입 본인 확인.
- `GET /api/v1/students/search`에 대한 요청 빈도 제한 등 추가 보안 완화책(`ADR-009` Risks에 위험으로만 기록).
- 기존 출결/숙제 API에 "본인/자녀 조회" 권한을 여는 것(`ADR-008`이 이미 별도 후속 작업으로 명시).

## Functional Scenarios

1. 지원자가 `role=TEACHER`로 `POST /api/v1/auth/signup`을 호출하면 `PENDING` 상태의 `User`가 생성된다. 관리자가 `POST /api/v1/users/{id}/approve`를 호출하면 `ACTIVE`로 전환되고 로그인할 수 있게 된다.
2. 지원자가 `GET /api/v1/students/search?name=김`으로 학생을 검색해 `studentId`를 찾은 뒤, `role=STUDENT`와 그 `studentId`로 회원가입하면 `PENDING` 상태의 `User`(`studentId` 설정됨)가 생성된다. 활성 선생님이 승인하면 `ACTIVE`로 전환된다.
3. 지원자가 `role=PARENT`와 `studentId`로 회원가입하면 `PENDING` 상태의 `User`와 `ParentStudentJpaEntity` 행이 함께 생성된다. 활성 선생님이 승인하면 `ACTIVE`로 전환된다.
4. 이미 다른 사용자가 클레임한(대기 또는 활성) `studentId`로 `STUDENT` 회원가입을 시도하면 실패한다.
5. 선생님이 대기 중인 학생 가입을 거절하면 해당 `User` 행이 삭제되고, 같은 `studentId`로 다시 회원가입할 수 있게 된다.
6. `PENDING` 상태의 계정으로 로그인을 시도하면 `AuthService`가 이미 `ACTIVE`만 허용하므로 401이 반환된다(기존 로직 재사용, 추가 구현 불필요).
7. 관리자가 대기 중인 선생님 가입을 거절하면 해당 `User` 행이 삭제된다.

## Business Rules

- `studentId`는 `STUDENT` 역할을 가진 `User`에만 설정될 수 있고, `childStudentIds`는 `PARENT` 역할을 가진 `User`에만 존재할 수 있다(`ADR-008` §5 원칙 재확인, Application Service에서 검증).
- 하나의 `Student`는 동시에 하나의 대기/활성 `STUDENT` 계정에만 연결될 수 있다(`users.student_id` UNIQUE 제약으로 강제).
- `approve`/`reject`는 `PENDING` 상태의 계정에만 적용할 수 있다.
- `TEACHER` 대기 계정의 승인/거절은 `ADMIN`만 가능하다.
- `STUDENT`/`PARENT` 대기 계정의 승인/거절은 `ACTIVE` 상태의 `TEACHER`만 가능하다.
- 거절은 `UserStatus`에 새 값을 추가하지 않고 대상 `User` 행(과 그에 딸린 `ParentStudentJpaEntity` 행)을 삭제하는 것으로 처리한다.
- `ADMIN` 역할은 회원가입 대상에서 제외된다 — `POST /api/v1/auth/signup`이 `role=ADMIN`을 받으면 거부한다.

## API Changes

### `POST /api/v1/auth/signup` (인증 불필요)

요청:
```json
{
  "loginId": "string",
  "password": "string",
  "name": "string",
  "role": "TEACHER | STUDENT | PARENT",
  "studentId": "number | null"
}
```
- `role=STUDENT` 또는 `PARENT`일 때 `studentId` 필수, `role=TEACHER`일 때 `studentId`는 무시(값이 오면 400).

응답: `201 Created`, `UserResponse`(아래 참고, `status=PENDING`).

### `GET /api/v1/students/search?name=` (인증 불필요)

응답: `200 OK`
```json
[{ "id": 123, "name": "김민준" }]
```
`id`, `name` 외 다른 필드(재원 상태 등)는 노출하지 않는다.

### `GET /api/v1/users/pending?role=TEACHER|STUDENT_PARENT`

- `ADMIN`: `role=TEACHER`만 조회 가능.
- `ACTIVE` `TEACHER`: `role=STUDENT_PARENT`만 조회 가능(STUDENT/PARENT 대기 계정을 함께 반환).
- 호출자 역할과 `role` 파라미터가 맞지 않으면 403.

응답: `200 OK`, `UserResponse[]`.

### `POST /api/v1/users/{id}/approve`

- 대상이 `TEACHER`면 `ADMIN`만, `STUDENT`/`PARENT`면 `ACTIVE` `TEACHER`만 호출 가능(그 외 403).
- 대상이 `PENDING`이 아니면 409 또는 400.
- 성공 시 `200 OK`, 갱신된 `UserResponse`(`status=ACTIVE`).

### `POST /api/v1/users/{id}/reject`

- 승인과 동일한 권한 규칙.
- 성공 시 `204 No Content`. 대상 `User` 행 및 `ParentStudentJpaEntity` 행 삭제.

### `UserResponse` 변경

기존 필드(`id`, `loginId`, `roles`, `status`)에 `name: String` 추가.

## Domain Impact

- `user/domain/User.kt`
  - 필드 추가: `name: String`, `studentId: Long?`.
  - `validate()`에 `name` 검증 추가(`loginId`와 동일한 스타일: not blank, 길이 제한).
  - `register(...)`에 `name`, `studentId` 파라미터 추가; `reconstitute(...)`도 동일하게 확장.
  - 새 도메인 메서드 `approve()`: `status`가 `PENDING`일 때만 `ACTIVE`로 전환, 아니면 예외.
  - 신규 `childStudentIds: Set<Long>` 필드는 `Classroom.studentIds`와 동일한 패턴(`private set`, `claimChild(studentId)` 같은 메서드로 추가 — 가입 시 1회만 호출).
- `user/domain/UserStatus.kt`: `PENDING` 값 추가.
- `user/infrastructure/`
  - `UserJpaEntity.kt`: `name`, `student_id` 컬럼 추가.
  - 신규 `ParentStudentJpaEntity.kt`(`id`, `parentUserId`, `studentId`) — `classroom/infrastructure/ClassStudentJpaEntity.kt` 패턴을 학부모-학생 연결 의미에 맞게 명확히 이름 붙인다.
  - 신규 `ParentStudentJpaRepository.kt`(`findByParentUserId`) — `ClassStudentJpaRepository.kt` 패턴.
  - `UserRepositoryAdapter.kt`: `toEntity`/`toDomain`에 `name`/`studentId` 매핑 추가, `syncRoles`와 동일한 방식으로 `childStudentIds`를 동기화하는 `syncParentStudents` 추가.
- `student/application` 또는 신규 `user/application` 서비스: `GET /api/v1/students/search`를 위한 최소 프로젝션 조회(기존 `StudentRepository`에 `findByNameContaining` 유사 메서드 추가, 없다면 신설).

## Database Impact

신규 Flyway 마이그레이션 `V6__add_user_signup_fields.sql`:

```sql
ALTER TABLE users ADD COLUMN name VARCHAR(100) NOT NULL DEFAULT '';
ALTER TABLE users ADD COLUMN student_id BIGINT NULL;
ALTER TABLE users ADD CONSTRAINT uq_users_student_id UNIQUE (student_id);

CREATE TABLE parent_student_links (
    id BIGINT PRIMARY KEY,
    parent_user_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL
);
```

- `status` 컬럼이 CHECK 제약 등으로 값이 고정되어 있다면 `PENDING`을 허용하도록 함께 수정한다(현재 마이그레이션 파일 확인 후 반영).
- 기존 데이터에 대한 `name` 백필 전략: 개발 초기 단계로 실제 운영 데이터가 없다면 빈 문자열 기본값으로 충분한지 구현 시점에 재확인한다(열려 있는 질문으로 남김).

## Exception and Error Handling

- 중복 `loginId`: 기존 `DuplicateLoginIdException` 재사용.
- 중복 `studentId` 클레임: 신규 예외(예: `DuplicateStudentClaimException`) → 409.
- `role=ADMIN`으로 가입 시도: 400.
- `role=STUDENT`/`PARENT`인데 `studentId` 누락, 또는 `role=TEACHER`인데 `studentId` 존재: 400.
- 존재하지 않는 `studentId`로 가입 시도: 기존 `Student` not-found 패턴 재사용 → 404 또는 400(Application Service에서 존재 검증, `ARCHITECTURE.md` §6.1).
- 대상이 `PENDING`이 아닌데 승인/거절 시도: 409.
- 권한과 대상 역할이 맞지 않는 승인/거절 시도(예: 선생님이 선생님 가입을 승인): 403.

## Test Scenarios

- 역할별(TEACHER/STUDENT/PARENT) 회원가입 정상 동작, `PENDING` 상태로 생성됨을 확인.
- `role=ADMIN` 회원가입 거부.
- 동일 `studentId`로 두 번째 `STUDENT` 회원가입 시도 시 거부.
- `role=STUDENT`인데 `studentId` 누락 시 400.
- `PENDING` 상태 계정으로 로그인 시도 시 401(기존 `AuthService` 로직으로 커버되는지 회귀 테스트로 확인).
- 관리자가 `TEACHER` 대기 계정 승인 → `ACTIVE` 전환, 로그인 가능해짐.
- 활성 선생님이 `STUDENT`/`PARENT` 대기 계정 승인 → `ACTIVE` 전환 및 `studentId`/`childStudentIds` 그대로 유지됨을 확인.
- 선생님이 `TEACHER` 대기 계정 승인 시도 시 403.
- 관리자가 `STUDENT`/`PARENT` 대기 계정 승인 시도 시 403(설계상 관리자는 이 경로를 쓰지 않음 — `ADR-009` Decision 3에 따라 명시적으로 막을지, 관리자도 허용할지는 구현 시 `ADR-009` 문구를 "활성 선생님만" 그대로 따를지 재확인 필요 — 열려 있는 질문으로 남김).
- 거절 시 `User` 행과 연관 `ParentStudentJpaEntity` 행 삭제, 이후 동일 `studentId`로 재가입 가능함을 확인.
- `GET /api/v1/students/search`가 `id`/`name`만 반환하고 다른 필드를 노출하지 않음을 확인.

## Acceptance Criteria

- 선생님/학생/학부모가 인증 없이 회원가입할 수 있다.
- 관리자 개입 없이도 활성 선생님이 학생/학부모 가입을 승인할 수 있다.
- 승인 전 계정은 로그인할 수 없다.
- `ADR-008`이 결정한 `studentId`/`childStudentIds` 연결이 실제로 구현되고, 가입-승인 흐름을 통해 생성된다.
- 기존 관리자 전용 계정 생성(`POST /api/v1/users`) 동작은 변경되지 않는다.
- 모든 신규 엔드포인트에 대한 인가 규칙이 `SecurityConfig.kt`에 반영되고, 기존 `/api/v1/students/**`, `/api/v1/users/**` catch-all 규칙보다 먼저 선언된다.

## Definition of Done

- 위 Domain/Database/API 변경이 구현되고, 나열된 테스트 시나리오가 모두 통과한다.
- `./gradlew test`(또는 프로젝트의 표준 검증 커맨드)가 통과한다.
- `docs/USER_ROLES.md` 4장의 "해소된 항목"에 이 TASK로 해소된 항목(있다면)을 반영한다.
- Implementation Agent의 self-check 이후, 별도의 독립적인 Review Agent 검토를 거친다(`AGENTS.md`, `CLAUDE.md` 원칙).
