# ADR-008: 학생 계정은 선택적 1:1, 학부모-학생 연결은 다대다로 관리한다

## Status

Accepted (2026-08-12)

이 ADR은 사용자가 세션 중 직접 결정했다(`ADR-002`, `ADR-006`, `ADR-007`과 동일한 방식) — 두 가지 질문(학생 본인 계정의 필수 여부, 학부모-학생 연결의 카디널리티)에 대해 사용자가 그 자리에서 답했다.

## Date

2026-08-12

## Context

- `REQUIREMENTS.md` §5, `USER_ROLES.md` §4, `DOMAIN_MODEL.md` §6은 공통적으로 "학부모와 학생의 연결 방식"을 미정 항목으로 남겨왔다. 이 때문에 `TASK-006`(출결), `TASK-007`(숙제) 모두 "학생 본인/학부모 자녀 조회" 권한을 Out of Scope로 명시적으로 제외했다.
- 현재 `User`(`TASK-003`)와 `Student`(`TASK-002`)는 서로를 전혀 참조하지 않는 완전히 분리된 Aggregate다 — `User`에는 `Student`를 가리키는 필드가 없고, `Student`에도 `User`를 가리키는 필드가 없다.
- `User`는 `ADMIN`/`TEACHER`/`STUDENT`/`PARENT` 네 가지 역할(`UserRole`)을 가지지만, `STUDENT`/`PARENT` 역할이 실제 `Student` 레코드와 어떻게 연결되는지는 지금까지 정의된 적이 없다.
- 이미 만들어진 출결/숙제 도메인을 포함해, 앞으로 만들 일정/학습 기록/공지 도메인까지 `REQUIREMENTS.md` 3.3~3.7 전 영역이 "학생 본인/학부모의 자녀 조회" 권한을 요구하므로, 이 연결 방식을 확정하지 않으면 해당 권한을 계속 구현할 수 없다.
- `ADR-006`은 반-학생/반-선생님 배정(다대다)을 `Classroom.studentIds`/`teacherIds` 형태의 순수 id 컬렉션으로, `ARCHITECTURE.md` §6.1 원칙에 따라 `@ElementCollection` 대신 `id + ownerId + value`만 가진 단순 JPA Entity(`ClassStudentJpaEntity`/`ClassTeacherJpaEntity`)로 구현한 선례가 있다.

## Decision

1. **모든 학생이 로그인 계정을 가질 필요는 없다.** `Student`는 연결된 `User` 계정 없이도 독립적으로 등록·조회·수정될 수 있다 — 관리자/선생님이 계정 존재 여부와 무관하게 학생을 관리하는 현재 흐름을 그대로 유지한다. 계정이 없는 학생에게는 "본인 조회" 권한이 적용되지 않는다(로그인 자체가 불가능하므로).
2. **학생 본인 계정 연결은 선택적 1:1이다.** `STUDENT` 역할을 가진 `User`는 최대 1개의 `Student`를 참조할 수 있다. `User`에 nullable `studentId: Long?` 필드를 추가하고, DB에는 `users.student_id BIGINT NULL` 컬럼과 UNIQUE 제약을 둔다(같은 학생을 두 계정이 동시에 참조하는 것을 방지). 물리 외래 키는 두지 않으며, 연결 시 대상 `Student`가 실제 존재하는지는 Application Service에서 검증한다(`ARCHITECTURE.md` §6.1).
3. **학부모-학생 연결은 다대다다.** 한 학부모 계정이 여러 자녀를 가질 수 있고, 한 학생에 여러 보호자(예: 부모 모두)가 연결될 수 있다. `ADR-006`의 `Classroom.studentIds` 패턴을 그대로 따라, `PARENT` 역할을 가진 `User`가 `childStudentIds: Set<Long>` 형태로 노출하고, 내부적으로는 `@ElementCollection` 대신 `id + userId + studentId`만 가진 단순 JPA Entity(예: `UserChildJpaEntity`)로 저장한다.
4. **두 연결 모두 관리자만 생성·해제할 수 있다.** `Classroom`-`Student` 배정, `Classroom`-`Teacher` 배정과 동일한 권한 주체 원칙을 그대로 적용한다.
5. **역할과 연결 필드의 정합성은 Application Service가 검증한다.** `studentId`는 `STUDENT` 역할이 없는 `User`에 설정될 수 없고, `childStudentIds`는 `PARENT` 역할이 없는 `User`에 추가될 수 없다 — 다만 이 검증 로직의 정확한 배치와 세부 API는 이 ADR의 범위가 아니며, 이후 작성할 TASK 문서에서 확정한다.

## Decision Drivers

- 실제 학원 현장에서는 저학년 학생처럼 본인 계정 없이 학부모 계정으로만 관리되는 경우가 흔하다 — 사용자가 이 요구를 명시적으로 확인했다.
- 형제자매가 있는 가정, 부모 모두 계정을 갖는 가정은 드물지 않다 — 1:1로 단순화하면 실제 요구사항을 만족하지 못한다.
- `ADR-006`이 이미 "다대다 배정은 소유 측 Aggregate가 id 컬렉션으로 관리하고, `@ElementCollection` 대신 단순 row 엔티티를 쓴다"는 패턴을 확립해 두어, 같은 패턴을 재사용하면 코드 일관성이 유지되고 새로운 개념(별도 Aggregate 등)을 추가하지 않아도 된다.
- 학생 본인 연결은 성격상 다대다가 아니라 "계정 1개당 학생 최대 1명"이 자연스러운 카디널리티라, 학부모 연결과 다른 필드로 분리하는 것이 검증 로직을 단순하게 유지한다.

## Considered Alternatives

### 1. 모든 학생에게 계정을 필수로 강제

- 설명: `Student` 등록 시 `User`(STUDENT) 계정 생성을 항상 함께 강제.
- 기각 사유: 사용자가 "계정 없이도 선생님이 학생을 관리할 수 있어야 한다"고 명시적으로 확인했다. 필수로 시작하면 이후 완화하는 것보다, 지금 선택적으로 열어두고 나중에 필요 시 좁히는 편이 되돌리기 쉽다.

### 2. 학부모-학생 연결을 1학부모-1자녀로 단순화

- 설명: `User`(PARENT)에 단일 nullable `studentId` 필드만 둔다.
- 기각 사유: 형제자매나 양쪽 부모 계정처럼 흔한 현실 사례를 표현할 수 없다 — 사용자가 다대다를 명시적으로 선택했다.

### 3. 학부모-학생 연결을 별도 Aggregate(예: `Guardianship`)로 분리

- 설명: `parentUserId`, `studentId`만 가진 독립 Aggregate Root를 신설.
- 기각 사유: 이 연결 자체에 별도 생명주기나 비즈니스 규칙(예: 승인 절차, 만료일)이 없다 — 현재 요구사항 수준에서는 `Classroom.studentIds`와 동일하게 `User`의 컬렉션으로 표현하는 것으로 충분하며, 새 Aggregate를 만드는 것은 근거 없는 과설계다. 승인/만료 같은 규칙이 실제로 필요해지면 그때 재검토한다.

### 4. (채택) 학생 연결은 `User.studentId`(선택적 1:1), 학부모 연결은 `User.childStudentIds`(다대다, `Classroom` 패턴 재사용)

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 결정.

## Consequences

### Positive

- `TASK-006`/`TASK-007`이 Out of Scope로 미뤄뒀던 "학생 본인/학부모 자녀 조회" 권한을 이후 TASK에서 소급 구현할 수 있는 근거가 마련된다.
- 앞으로 만들 일정/학습 기록/공지 도메인도 처음부터 이 연결을 전제로 설계할 수 있다.
- 기존 `Classroom.studentIds` 패턴을 재사용해 새로운 아키텍처 개념을 추가하지 않는다 — 코드 일관성이 유지된다.

### Negative

- `User` Aggregate에 역할별로만 의미 있는 필드(`studentId`는 STUDENT 전용, `childStudentIds`는 PARENT 전용)가 함께 존재하게 되어, 역할-필드 정합성 검증을 Application Service가 계속 책임져야 한다.
- 이미 만들어진 출결/숙제 API에 "본인/자녀 조회" 엔드포인트를 추가하는 후속 작업이 별도로 필요하다 — 이 ADR만으로는 실제 조회 권한이 열리지 않는다.

### Risks

- 학부모 계정이 실수로 다른 가정의 자녀를 연결하는 것을 막는 절차(예: 학생 측 동의, 관리자 확인 절차의 구체적 형태)는 이 ADR에서 다루지 않았다 — 관리자 전용 생성으로 우선 통제하되, 운영 중 오배정 리스크가 발견되면 재검토한다.
- `studentId`/`childStudentIds`의 역할-필드 정합성 검증이 빠지면 데이터 정합성이 깨질 수 있다 — 후속 TASK에서 반드시 구현해야 한다.

## Validation

- 이 ADR을 근거로 후속 TASK 문서(User-Student/Parent 연결 API, 기존 출결/숙제 API의 본인/자녀 조회 확장)를 작성할 때 이 구조와 맞지 않는 요구사항이 발견되면 코드를 먼저 바꾸지 않고 이 ADR의 갱신 여부를 먼저 논의한다.

## Related Documents

- `docs/ARCHITECTURE.md` §6.1 (Aggregate 간 id 참조 원칙, `@ElementCollection` 금지)
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md` (동일 패턴의 선례 — `Classroom.studentIds`/`teacherIds`)
- `docs/DOMAIN_MODEL.md` §3.1, §3.2, §4, §6
- `docs/REQUIREMENTS.md` §5
- `docs/USER_ROLES.md` §4
- `docs/tasks/TASK-006-attendance-management.md`, `docs/tasks/TASK-007-homework-management.md` (Out of Scope로 미뤄둔 항목의 근거)
- `docs/adr/ADR-009-user-signup-and-approval.md` — Decision 4("두 연결 모두 관리자만 생성·해제할 수 있다")를 확장해, 선생님의 회원가입 승인도 연결을 생성하는 유효한 경로로 추가한다. 학부모-학생 연결을 저장하는 JPA Entity는 `TASK-008` 구현 시 `ParentStudentJpaEntity`(`id + parentUserId + studentId`)라는 이름으로 실제 도입되었다 — 이 Decision 3이 예시로 든 `UserChildJpaEntity`라는 이름 자체는 채택되지 않았다.
