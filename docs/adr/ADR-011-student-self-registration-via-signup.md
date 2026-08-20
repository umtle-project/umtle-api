# ADR-011: 학생 자가 회원가입 시 Student 마스터 데이터를 함께 생성한다

## Status

Proposed

## Date

2026-08-21

## Context

`TASK-008`이 구현한 자가 회원가입 흐름은 `STUDENT`/`PARENT` 역할 모두 반드시 이미 존재하는 `Student` 레코드를 `studentId`로 연결해야만 가입이 성립한다고 전제했다 — "학생/학부모로 가입하려는 지원자가 연결하려는 `Student` 레코드가 이미 관리자에 의해 등록되어 있어야 한다"(`docs/tasks/TASK-008-user-signup-and-approval.md:38`). `Student` 등록은 `POST /api/v1/students`로 `ADMIN` 역할만 수행할 수 있다(`config/SecurityConfig.kt`의 `/api/v1/students/**` → `hasRole("ADMIN")`).

이 전제 때문에, 학원에 한 번도 등록된 적 없는 학생은 스스로 회원가입할 방법이 없다 — 관리자가 먼저 `Student`를 만들어 줘야만 그 이름을 검색해 가입할 수 있다.

사용자는 역할별 서비스 목적을 다음과 같이 명확히 했다:
- **관리자(Admin)**: 전체 데이터에 대한 백오피스 관리.
- **선생님(Teacher)**: 학생, 수업, 일정 관리.
- **학생(Student)**: 본인에게 배정된 숙제/일정을 스스로 관리하는 주체.

이 목적에 따르면 학생은 관리자/선생님의 개입 없이 스스로 학원 시스템에 진입(회원가입)할 수 있어야 한다. 현재 흐름은 이를 막고 있다.

이 결정을 내리기 전, "관리자/선생님은 `User`만으로 관리되고, `Student`는 `User`와 분리된 별도 도메인으로 존재한다"는 기존 구조(`ADR-008`) 자체가 여전히 올바른지 재검토했다:
- Admin/Teacher는 회원가입 승인, 출결 기록, 숙제 배정 등 모든 업무 수행에 로그인이 필수이므로 "계정 없는 선생님"이라는 비즈니스 케이스가 없다 — `User` 단독으로 충분하다. 실제로 `Classroom.teacherIds`도 `Student.id`가 아니라 `User.id`를 참조한다(`src/main/kotlin/com/umtle/umtleapi/classroom/domain/Classroom.kt:21,41-42`).
- 반면 `Student`는 계정 없이 존재해야 하는 실제 요구가 있다 — 저학년 학생처럼 본인 계정 없이 학부모 계정으로만 관리되는 경우가 흔하다(`docs/adr/ADR-008-user-student-parent-connection.md:31`). `Classroom`/`Attendance`/`Homework`(`TASK-002`, `TASK-005`~`007`)가 이미 `User.id`가 아닌 `Student.id`를 기준 정보로 참조하고 있다(`docs/DOMAIN_MODEL.md:55`).
- 따라서 `Student`를 `User`에 흡수하는 것은 근거가 없다 — 이번 결정은 `Student`/`User` 분리 구조를 유지한 채, `Student`를 "누가, 언제" 만들 수 있는지만 넓힌다.

## Decision

1. **`STUDENT` 역할 자가 회원가입 시 `studentId`를 선택적으로 만든다.**
   - `studentId`가 주어지면: 기존 흐름 그대로(`Student` 존재 검증, 중복 클레임 검증 후 연결). 관리자가 이미 등록해 둔 학생을 검색해 연결하는 경로는 그대로 유지된다.
   - `studentId`가 생략되면: 회원가입 트랜잭션 안에서 `Student.register(name)`으로 새 `Student`를 생성하고, 그 id를 발급받은 `User`에 연결한다. 관리자의 사전 등록 없이도 학생 스스로 회원가입할 수 있게 된다.
2. **`PARENT` 역할 자가 회원가입은 변경하지 않는다.** `studentId`는 여전히 필수이며, 학부모는 기존과 동일하게 `GET /api/v1/students/search`로 이미 존재하는 자녀의 `Student`를 찾아 연결한다. 학부모가 자녀용 `Student`를 새로 만드는 것은 이번 결정의 범위가 아니다.
3. **관리자 수동 학생 등록(`POST /api/v1/students`, `ADMIN` 전용)은 그대로 유지한다.** 자가등록은 기존 경로를 대체하는 것이 아니라 추가되는 경로다 — 오프라인 등록, 계정을 갖지 않는 저학년 학생 관리(`ADR-008` 핵심 시나리오)를 위해 계속 필요하다.
4. **자가등록이 거절(reject)되어도 그때 생성된 `Student`는 삭제하지 않는다.** `rejectUser()`는 기존과 동일하게 `User` 행만 삭제한다(`TASK-008` rule 5 그대로 재사용). `Student`는 `User`와 독립적인 생명주기를 갖는 기준 정보이므로, 거절 시점에 이미 다른 `PARENT`가 그 `Student`를 연결했을 수 있다는 점을 고려해 자동 삭제하지 않는다. 불필요해진 `Student`는 관리자가 기존 `POST /api/v1/students/{id}/deactivate`로 수동 정리한다.

## Decision Drivers

- 학생이 관리자/선생님의 개입 없이도 시스템에 스스로 진입할 수 있어야 한다는 사용자의 명시적 요구 — 역할별 서비스 목적(Admin=백오피스, Teacher=학생/수업/일정 관리, Student=본인 숙제/일정 자기관리)에 따른 것.
- `Student`/`User` 분리 구조(`ADR-008`)는 여전히 유효하다는 재검토 결과 — 흡수·재설계가 아니라 생성 경로 확장만으로 목표를 달성할 수 있다.
- 기존 `TASK-008` 흐름(승인/거절, 중복 클레임 방지)과 `ARCHITECTURE.md` §6.1(Application Service에서 존재 검증) 원칙을 그대로 재사용해 새로운 개념을 추가하지 않는다.

## Considered Alternatives

### 1. `Student`를 `User`에 흡수해 단일 엔티티로 통합

- 설명: 별도 `Student` 도메인을 없애고, 학생 정보를 `User`(role=STUDENT)에 직접 저장.
- 기각 사유: 계정 없는 학생(저학년 등, `ADR-008` 핵심 시나리오)을 표현할 수 없게 된다. `Classroom`/`Attendance`/`Homework`가 이미 `Student.id`를 기준 FK로 삼고 있어 마이그레이션 비용이 크고, 로그인 불가능한 학생을 위해 loginId/password가 없는 반쪽짜리 `User` 행이 필요해져 Spring Security의 "`User` = 인증 주체" 전제와 충돌한다.

### 2. 자가등록 거절 시 함께 생성된 `Student`도 삭제

- 설명: `rejectUser()`가 `User` 삭제와 함께, 이번 자가등록으로 새로 만들어진 `Student`도 삭제.
- 기각 사유: 거절 시점에 이미 다른 `PARENT`가 그 `Student`를 `childStudentIds`로 연결했을 수 있어 참조가 끊어질 위험이 있다. `Student`가 `User`와 독립적인 기준 정보라는 `ADR-008` 원칙과도 맞지 않는다. 남은 `Student`는 기존 `deactivate` API로 관리자가 수동 정리하는 것으로 충분하다.

### 3. 관리자 수동 학생 등록 경로 폐지, 자가등록으로 일원화

- 설명: `POST /api/v1/students`(ADMIN 전용)를 없애고, `Student`는 오직 회원가입을 통해서만 생성.
- 기각 사유: 계정을 갖지 않는 학생 관리라는 `ADR-008`의 핵심 요구를 깨뜨린다 — 저학년 학생, 오프라인 등록 등은 여전히 관리자가 직접 등록해야 한다.

### 4. (채택) `Student`/`User` 분리 구조 유지, `STUDENT` 자가등록에 신규 `Student` 생성 경로 추가

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 결정.

## Consequences

### Positive

- 학생이 관리자/선생님의 사전 등록 없이도 스스로 회원가입할 수 있게 된다.
- 기존 `studentId` 지정 흐름(관리자 사전 등록 + 검색 연결)과 `PARENT` 흐름은 완전히 하위 호환된다 — 기존 동작에 회귀가 없다.
- `Student`/`User` 분리 구조, 중복 클레임 방지, 승인/거절 로직 등 `TASK-008`/`ADR-008`이 확립한 원칙을 그대로 재사용한다.

### Negative

- 자가등록이 거절된 후에도 그때 생성된 `Student`가 로스터에 남는다 — 관리자가 수동으로 정리해야 하는 운영 부담이 생긴다.

### Risks

- 스팸/오입력 자가등록이 반복되면 `Student` 로스터가 오염될 수 있다. Rate limiting 등 완화책은 `TASK-008`(`ADR-009` Risks)과 동일하게 이번 결정의 범위 밖으로 남긴다.

## Validation

자가등록으로 생성된 `Student`가 실제로 로스터를 오염시키는 사례가 관찰되면(예: 거절률이 높은데 미정리 `Student`가 누적되는 경우), 거절 시 `Student` 정리 정책(자동 비활성화 등)을 재검토한다. Not applicable 수준의 고정 주기 리뷰는 없음.

## Related Documents

- `docs/adr/ADR-008-user-student-parent-connection.md` — `Student`/`User` 연결 구조의 원 결정.
- `docs/adr/ADR-009-user-signup-and-approval.md` — 자가 회원가입/승인 정책의 원 결정.
- `docs/tasks/TASK-008-user-signup-and-approval.md` — 이번 결정이 갱신하는 전제(§13, §38)를 구현한 기존 TASK.
- `docs/tasks/TASK-011-student-self-registration.md` — 이 ADR을 구현하는 TASK.
- `docs/DOMAIN_MODEL.md` §3.2 — `Student`의 책임 정의.
