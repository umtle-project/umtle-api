# TASK-011: 학생 자가 회원가입 시 Student 마스터 데이터 자동 생성

## Status

Merged — `main`에 PR #22(구현), #25(회원가입 승인과 학생 상태 동기화 수정)로 머지 완료(2026-08-21).

## Purpose

`STUDENT` 역할로 자가 회원가입하는 지원자가, 관리자가 미리 `Student`를 등록해 두지 않아도 스스로 학원 시스템에 진입할 수 있게 한다. `studentId`를 생략하면 회원가입 과정에서 새 `Student`가 함께 생성된다.

## Background

`TASK-008`이 구현한 자가 회원가입 흐름은 `STUDENT`/`PARENT` 모두 반드시 이미 존재하는 `Student`를 검색해 `studentId`로 연결해야만 가입이 성립한다고 전제했다(`docs/tasks/TASK-008-user-signup-and-approval.md:38`). `Student` 등록(`POST /api/v1/students`)은 `ADMIN` 전용이라, 학원에 한 번도 등록된 적 없는 학생은 스스로 회원가입할 방법이 없었다.

사용자는 역할별 서비스 목적(관리자=백오피스, 선생님=학생/수업/일정 관리, 학생=본인 숙제/일정을 스스로 관리하는 주체)을 명확히 했고, 이에 따라 학생이 관리자/선생님의 사전 등록 없이 스스로 회원가입할 수 있어야 한다고 요구했다. `ADR-011`이 이 요구를 결정했고, 이 TASK가 그 결정을 구현한다.

## Related Documents and Requirement IDs

- `docs/adr/ADR-011-student-self-registration-via-signup.md` — 이 TASK가 구현하는 결정
- `docs/adr/ADR-008-user-student-parent-connection.md` — 재사용하는 `Student`/`User` 연결 구조(`studentId` 선택적 1:1)
- `docs/tasks/TASK-008-user-signup-and-approval.md` — 이 TASK가 확장하는 기존 회원가입/승인 흐름
- `docs/DOMAIN_MODEL.md` §3.2 (학생)

## Users and Permissions

| 역할 | 권한 |
|------|------|
| 비인증(익명) | 회원가입 신청(`POST /api/v1/auth/signup`) — `role=STUDENT`일 때 `studentId` 생략 가능해짐(변경) |
| 관리자 (Admin) | 기존 `POST /api/v1/students`를 통한 수동 학생 등록(변경 없음) |
| 선생님 (Teacher) | 대기 중인 학생 계정 승인·거절(변경 없음, `TASK-008` 재사용) |
| 학생 (Student) | 이 TASK로 새로 부여되는 권한 없음(회원가입 자체는 비인증 상태에서 이루어짐) |
| 학부모 (Parent) | 변경 없음 — 기존과 동일하게 이미 존재하는(자가등록으로 생성된 것 포함) `Student`를 검색해 연결 |

## Preconditions

- `TASK-008`(자가 회원가입 및 역할별 승인)이 구현되어 있어야 한다.
- `TASK-002`(학생 관리, `Student.register`)가 구현되어 있어야 한다.
- `ADR-011`이 `Accepted` 상태로 전환되어 있어야 한다.

## Scope

- `POST /api/v1/auth/signup`에서 `role=STUDENT`일 때 `studentId`를 선택적으로 허용한다(DTO 자체는 이미 `studentId: Long?`로 nullable이라 `UserDtos.kt` 변경은 불필요 — 서비스 레벨 검증만 바뀐다).
- `UserService.signup()`의 검증/생성 로직을 다음과 같이 확장한다:
  - `role == STUDENT && studentId == null`: `Student.register(name)`으로 새 `Student`를 생성·저장하고, 그 id를 발급받은 `User`의 `studentId`로 사용한다. 기존 `studentRepository`가 `UserService`에 이미 주입되어 있으므로 재사용한다(`src/main/kotlin/com/umtle/umtleapi/user/application/UserService.kt:20`).
  - `role == STUDENT && studentId != null`: 기존 흐름 그대로 — `Student` 존재 검증(`StudentNotFoundException`), 중복 클레임 검증(`DuplicateStudentClaimException`) 후 연결.
  - `role == PARENT`: 변경 없음, `studentId` 필수.
  - `role == TEACHER`: 변경 없음, `studentId`는 여전히 허용되지 않음(값이 오면 400).
- `rejectUser()`는 변경하지 않는다 — 기존과 동일하게 `User` 행만 삭제하고, 자가등록으로 생성된 `Student`는 그대로 둔다.

## Out of Scope

- `PARENT`가 자녀용 `Student`를 새로 생성하는 흐름(자녀는 여전히 기존 `Student`를 검색해서만 연결).
- 자가등록 거절 시 생성된 `Student`를 자동으로 비활성화/삭제하는 정리 로직 — 필요해지면 관리자가 기존 `POST /api/v1/students/{id}/deactivate`로 수동 처리한다(`ADR-011` Decision 4).
- `Student` 로스터 오염(스팸/중복 자가등록) 방지책(rate limiting 등) — `TASK-008`과 동일하게 범위 밖.
- 학생/학부모의 숙제·일정 셀프서비스 조회·관리 API — 현재 `homeworks`/`lessons`/`attendances`는 전부 `TEACHER`/`ADMIN` 전용이며(`config/SecurityConfig.kt:35-46`), 이를 여는 것은 완전히 별도의 후속 TASK다.
- `PENDING` 상태 개념을 `Student`에 도입하는 것 — `Student`는 현재도 `ACTIVE`/`INACTIVE`만 가지며(`student/domain/Student.kt`), 이 TASK도 그 구조를 바꾸지 않는다.

## Functional Scenarios

1. 지원자가 `studentId` 없이 `role=STUDENT`, `name="김민준"`으로 `POST /api/v1/auth/signup`을 호출하면, 새 `Student`(`name="김민준"`, `status=ACTIVE`)와 `PENDING` 상태의 `User`(그 `Student`의 id로 `studentId` 설정됨)가 함께 생성된다.
2. 이후 활성 선생님이 그 `User`를 승인하면 기존 `TASK-008` 흐름과 동일하게 `ACTIVE`로 전환된다.
3. 지원자가 (기존과 동일하게) `GET /api/v1/students/search`로 이미 등록된 학생을 찾아 그 `studentId`로 `role=STUDENT` 회원가입하면, 기존 흐름 그대로 동작한다(신규 `Student` 생성 없음).
4. 자가등록으로 생성된 `Student`도 다른 사람의 `PARENT` 회원가입 검색 대상에 즉시 포함된다 — 승인 대기 여부와 무관하게 `GET /api/v1/students/search`가 모든 `Student`를 대상으로 검색하는 기존 동작을 그대로 따른다(`student/application/StudentService.kt:23-26`에 상태 필터 없음).
5. 선생님이 자가등록(신규 `Student` 생성 포함)된 학생의 가입을 거절하면, 기존과 동일하게 `User` 행만 삭제되고 `Student`는 남는다 — 이후 관리자가 그 `Student`로 새 회원가입을 승인하거나 수동으로 비활성화할 수 있다.
6. `role=PARENT` 회원가입은 이 TASK로 아무 것도 바뀌지 않는다 — `studentId` 누락 시 여전히 400.

## Business Rules

- `studentId`는 여전히 `STUDENT` 역할을 가진 `User`에만 설정될 수 있다(`ADR-008` §5, 변경 없음).
- `role=STUDENT`이고 `studentId`가 생략된 경우, 회원가입은 실패하지 않는다 — 대신 새 `Student`를 생성한다. 이는 `TASK-008` Business Rules의 "`role=STUDENT`/`PARENT`인데 `studentId` 누락 시 400" 규칙을 `STUDENT`에 한해 대체한다(`PARENT`는 그대로 400).
- 자가등록으로 생성된 `Student`도 `users.student_id` UNIQUE 제약(`TASK-008` 마이그레이션) 대상이다 — 다만 새로 생성된 `Student`는 그 시점에 어떤 `User`도 클레임하지 않은 상태이므로 중복 클레임 검증은 항상 통과한다.
- 승인/거절 권한 규칙은 `TASK-008`을 그대로 따른다(활성 `TEACHER`가 `STUDENT` 대기 계정을 승인/거절).
- 거절된 자가등록의 `Student`는 삭제되지 않는다(`ADR-011` Decision 4) — `User`만 삭제된다.

## API Changes

### `POST /api/v1/auth/signup` (인증 불필요) — 요청 검증 규칙 변경

```json
{
  "loginId": "string",
  "password": "string",
  "name": "string",
  "role": "TEACHER | STUDENT | PARENT",
  "studentId": "number | null"
}
```

- `role=STUDENT`: `studentId` 선택. 생략 시 `name`으로 새 `Student`를 생성해 연결(변경). 값이 주어지면 기존 검증 그대로.
- `role=PARENT`: `studentId` 필수(변경 없음).
- `role=TEACHER`: `studentId`는 여전히 허용되지 않음(값이 오면 400, 변경 없음).

응답: 변경 없음 — `201 Created`, `UserResponse`(`status=PENDING`).

다른 엔드포인트(`/students/search`, `/users/pending`, `/users/{id}/approve`, `/users/{id}/reject`)는 이 TASK로 변경되지 않는다.

## Domain Impact

- `user/application/UserService.kt`
  - `signup()`: `role == STUDENT && studentId == null` 분기 추가 — `studentRepository.save(Student.register(name))`로 새 `Student`를 만들고 그 `id`를 `claimedStudentId`로 사용. 기존 `studentId != null` 분기(존재 검증, 중복 클레임 검증)는 그대로 유지.
- `student/domain/Student.kt`, `student/application/StudentService.kt`: 변경 없음 — 기존 `Student.register(name)` 팩토리를 그대로 재사용한다.
- `user/domain/User.kt`, `UserStatus.kt`, 인프라 계층: 변경 없음 — `TASK-008`이 이미 구현한 `studentId` 필드/검증을 그대로 재사용한다.

## Database Impact

없음 — 기존 `users.student_id`, `students` 테이블 스키마를 그대로 사용한다. 신규 마이그레이션 불필요.

## Exception and Error Handling

- `role=STUDENT`, `studentId` 없음: 예외 없이 성공 처리(신규 `Student` 생성). `name`이 비어 있거나 100자를 초과하면 `Student.register`의 기존 `require` 검증이 예외를 던진다(`student/domain/Student.kt:41-44`).
- `role=STUDENT`, `studentId` 있음 + 존재하지 않는 id: 기존과 동일하게 `StudentNotFoundException`.
- `role=STUDENT`, `studentId` 있음 + 이미 클레임됨: 기존과 동일하게 `DuplicateStudentClaimException`.
- `role=PARENT`, `studentId` 없음: 기존과 동일하게 `InvalidSignupRequestException` → 400(변경 없음).
- `role=TEACHER`, `studentId` 있음: 기존과 동일하게 `InvalidSignupRequestException` → 400(변경 없음).

## Test Scenarios

- `studentId` 없이 `role=STUDENT`로 회원가입 → `201`, 새 `Student`가 생성되고(`GET /api/v1/students/{id}`로 조회 가능) 응답의 `User`가 그 `studentId`를 갖는지 확인.
- `studentId`를 지정한 기존 `STUDENT` 회원가입 흐름이 회귀 없이 동작하는지 확인(`TASK-008` 기존 테스트 재실행).
- `role=PARENT` 회원가입은 여전히 `studentId` 필수이며, 생략 시 400인지 확인(회귀 테스트).
- 자가등록(신규 `Student` 생성)된 학생을 선생님이 거절 → `User`는 삭제되지만 `Student`는 `GET /api/v1/students/{id}`로 계속 조회 가능한지 확인.
- 자가등록으로 생성된 `Student`가 `GET /api/v1/students/search`로 검색되는지 확인(승인 대기 상태와 무관하게 검색됨을 확인).
- `studentId` 없이 `role=STUDENT`로 가입할 때 이름 검증(빈 문자열, 100자 초과)이 `Student.register`를 통해 그대로 적용되는지 확인.

## Acceptance Criteria

- 관리자/선생님의 사전 `Student` 등록 없이도 학생이 `POST /api/v1/auth/signup`만으로 회원가입할 수 있다.
- 기존에 `studentId`를 지정해 가입하는 흐름, `PARENT` 가입 흐름, 승인/거절 흐름은 모두 이 TASK 이전과 동일하게 동작한다(회귀 없음).
- 자가등록 거절 시 `User`만 삭제되고 `Student`는 남는다.
- 관리자 전용 수동 학생 등록(`POST /api/v1/students`) 동작은 변경되지 않는다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준을 따른다. 추가로:

- 위 Domain 변경(`UserService.signup()`)이 구현되고, 나열된 테스트 시나리오가 모두 통과한다.
- `./gradlew test`가 통과한다.
- Implementation Agent의 self-check 이후, 별도의 독립적인 Review Agent 검토를 거친다(`AGENTS.md`, `CLAUDE.md` 원칙).

## Open Questions

- 자가등록으로 생성된 `Student`의 로스터 오염(스팸/오입력) 방지 대책은 이번 TASK 범위 밖으로 남긴다 — 실제 운영에서 문제가 확인되면 별도 TASK로 다룬다(`ADR-011` Risks).

## Related ADRs

- `docs/adr/ADR-011-student-self-registration-via-signup.md`
- `docs/adr/ADR-008-user-student-parent-connection.md`
- `docs/adr/ADR-009-user-signup-and-approval.md`
