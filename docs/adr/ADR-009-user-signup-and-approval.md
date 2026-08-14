# ADR-009: 사용자 자가 회원가입과 역할별 승인 절차를 도입한다

## Status

Accepted (2026-08-14)

## Date

2026-08-14

## Context

- 현재 모든 사용자 계정은 인증된 `ADMIN`이 `POST /api/v1/users`(`UserController.kt`)를 통해서만 생성할 수 있다. `SecurityConfig.kt`는 `/api/v1/users/**` 전체를 `hasRole("ADMIN")`으로 막아두었고, 인증 없이 접근 가능한 회원가입 엔드포인트는 존재하지 않는다.
- `REQUIREMENTS.md`, `USER_ROLES.md` 어디에도 "회원가입"이라는 개념 자체가 서술된 적이 없다 — 이는 설계 결함이 아니라 지금까지 아예 결정된 적이 없는 항목이다.
- 사용자가 세션 중 직접 확인했다: 선생님, 학생, 학부모도 각자 자가 회원가입이 가능해야 하며, 관리자만 모든 계정을 생성하는 현재 구조로는 운영이 불가능하다.
- 이 논의 과정에서 별개의 실제 설계 공백도 함께 확인되었다: `User`(`user/domain/User.kt`)에는 이름(`name`)을 저장할 필드가 전혀 없다 — `loginId`, `passwordHash`, `roles`, `status`뿐이다. 자가 회원가입을 여는 것과 무관하게, 선생님·학부모 등을 화면에 표시하려면 반드시 채워야 하는 공백이다.
- `ADR-008`(Accepted, 2026-08-12)은 이미 `User`-`Student` 연결 구조를 결정했다 — `STUDENT` 역할의 `User`는 선택적으로 최대 1명의 `Student`를 `studentId`로 참조하고(`users.student_id BIGINT NULL` + UNIQUE), `PARENT` 역할의 `User`는 `ADR-006`의 `Classroom.studentIds` 패턴을 재사용한 `ParentStudentJpaEntity`(`id + parentUserId + studentId`) 행 엔티티로 여러 `Student`를 다대다로 참조한다. 그러나 이 결정을 구현하는 TASK 문서는 지금까지 작성된 적이 없다 — `User.kt`에는 `studentId`도 `childStudentIds`도 존재하지 않는다.
- `ADR-008`의 Decision 4는 "두 연결 모두 관리자만 생성·해제할 수 있다"고 명시했다. 이번 결정은 이 항목을 **대체하지 않고 확장**한다 — 관리자의 직접 생성·해제 권한은 그대로 유지하되, 선생님이 학생/학부모의 자가 회원가입을 승인하는 것 역시 같은 연결을 만드는 유효한 경로로 추가한다. `ADR-008`은 이미 `Accepted` 상태이므로 본문을 직접 고치지 않고, 이 ADR이 해당 결정을 참조하며 범위를 넓히는 방식을 택한다.

## Decision

1. **회원가입 가능 역할은 `TEACHER`, `STUDENT`, `PARENT` 세 가지뿐이다.** `ADMIN` 계정은 자가 회원가입 대상이 아니며, 지금처럼 관리자 전용 `POST /api/v1/users`로만 생성한다.
2. **모든 자가 회원가입 계정은 `UserStatus.PENDING` 상태로 생성된다.** `PENDING` 상태의 계정은 로그인할 수 없다(`AuthService.authenticate`가 `ACTIVE`만 허용하도록 이미 구현되어 있으므로, `PENDING` 추가만으로 자연히 로그인 차단이 성립한다).
3. **승인 주체는 대상 계정의 역할에 따라 다르다.**
   - `TEACHER` 역할로 가입한 대기 계정은 **관리자(`ADMIN`)**가 승인한다.
   - `STUDENT` 또는 `PARENT` 역할로 가입한 대기 계정은 **현재 `ACTIVE` 상태인 아무 선생님(`TEACHER`)**이나 승인할 수 있다. "담당 선생님/담당 반" 개념은 `USER_ROLES.md` 4장에서 여전히 미정이므로, 이 결정은 그 기준이 확정되기를 기다리지 않고 "활성 선생님 누구나"로 범위를 정한다. 담당 기준이 추후 확정되면 이 결정을 좁히는 후속 ADR을 검토한다.
4. **학생/학부모 가입은 승인과 동시에 `ADR-008` 연결을 확정한다.** `STUDENT`/`PARENT` 지원자는 가입 시점에 기존 `Student` 레코드를 검색해 하나를 선택하고(§ TASK-008의 학생 검색 API), 그 `studentId`를 가입 요청에 함께 제출한다. 이 값은 가입 즉시 `User.studentId`(STUDENT) 또는 신규 `ParentStudentJpaEntity` 행(PARENT)에 기록된다. `users.student_id` UNIQUE 제약은 동일 학생에 대한 두 개의 동시 대기/활성 학생 본인 계정 클레임을 막고, 학부모-자녀 연결은 한 학생에 여러 보호자가 연결될 수 있도록 `parent_student_links` 행으로 관리한다. 승인은 상태를 `ACTIVE`로 전환하는 것만으로 충분하고, 거절은 대기 중인 `User` 행(및 그에 딸린 `ParentStudentJpaEntity` 행)을 삭제해 클레임을 해제한다. 별도의 `REJECTED` 상태나 승인 대기용 별도 Aggregate(`SignupRequest` 등)를 새로 만들지 않는다.
5. **이 결정은 `ADR-008` Decision 4를 대체하지 않고 확장한다.** 관리자가 직접 `studentId`/`childStudentIds`를 생성·해제하는 기존 권한은 그대로 유지된다. 선생님의 승인 권한은 "회원가입 승인이라는 특정 행위의 부수 효과로 연결이 생성되는 것"으로 한정되며, 이미 활성 상태인 계정의 연결을 임의로 바꿀 수 있는 일반 권한이 아니다.
6. **학부모 가입은 가입 시점에 자녀 1명만 클레임한다.** 이미 승인된 학부모 계정에 자녀를 추가로 연결하는 자가 서비스 흐름은 이 ADR의 범위가 아니다 — 기존처럼 관리자가 직접 추가한다(`ADR-008` 유지).
7. **`User`에 표시용 이름 필드 `name`을 추가한다.** 모든 역할에 공통으로 필요하며, 회원가입 정책과 무관하게 확정된 결정이다.

## Decision Drivers

- 선생님은 이미 관리자가 검증한 학원 소속 인력이라는 신뢰 전제가 있어, 학생/학부모처럼 흔히 발생하는 가입 승인을 관리자에게만 몰아주면 병목이 된다.
- `ADR-006`(Classroom의 id 컬렉션 배정)과 `ADR-008`(User-Student 연결 구조)이 이미 확립한 패턴을 그대로 재사용하면, 승인 대기라는 새로운 상태를 위해 별도 Aggregate를 신설하지 않고도 요구사항을 만족할 수 있다.
- 사용자가 세션 중 "관리자는 그냥 전체 권한이라 생각하고"라고 명시했으므로, 관리자의 기존 전권(직접 생성/해제, 모든 승인에 대한 최종 개입 가능성)을 축소하지 않는 방향으로 설계한다.

## Considered Alternatives

### 1. 완전 공개 가입 (승인 절차 없이 즉시 활성화)

- 설명: 가입 즉시 `ACTIVE` 상태로 로그인 가능.
- 기각 사유: 학원이라는 폐쇄형 조직 특성상 승인 없는 가입은 아무나 학생 정보에 스스로를 연결할 수 있다는 뜻이 되어 리스크가 크다. 사용자가 역할별 승인 절차를 명시적으로 선택했다.

### 2. 초대 코드 기반 가입

- 설명: 관리자가 발급한 코드로만 가입 가능, 코드 자체에 역할·연결 대상이 미리 지정됨.
- 기각 사유: 사용자가 "가입자가 기존 Student를 검색/선택"하는 방식을 명시적으로 선택했다 — 코드 발급이라는 별도 관리자 작업 없이도 지원자 스스로 대상을 찾아 신청할 수 있는 현재 방식이 더 적합하다고 판단했다.

### 3. 승인 대기를 별도 `SignupRequest` Aggregate로 분리

- 설명: `User`와 별개로 `SignupRequest`(loginId, password, role, claimedStudentId 등)를 신설하고, 승인 시점에 비로소 `User`를 생성.
- 기각 사유: `UserStatus.PENDING`과 `ADR-008`의 기존 연결 필드(`studentId`/`childStudentIds`)를 그대로 재사용하는 것으로 요구사항을 충분히 만족한다 — 승인/거절이라는 단순한 생명주기를 위해 새 Aggregate와 그 마이그레이션 경로(승인 시 `SignupRequest` → `User` 변환)를 추가하는 것은 근거 없는 과설계다.

### 4. (채택) `User` + `PENDING` 상태 + `ADR-008` 연결 필드 재사용

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 결정.

## Consequences

### Positive

- 관리자 병목 없이 선생님/학생/학부모가 스스로 계정을 만들 수 있게 되어, 실제 운영에 필요한 최소 기능이 채워진다.
- `ADR-008`이 결정만 해두고 구현되지 않았던 `studentId`/`childStudentIds` 연결 필드가 이번 TASK를 통해 처음으로 구현된다.
- 새로운 아키텍처 개념(별도 Aggregate 등)을 추가하지 않고 `ADR-006`/`ADR-008`의 기존 패턴을 그대로 재사용해 코드 일관성이 유지된다.

### Negative

- `User`는 이제 역할에 따라서만 의미를 갖는 필드(`studentId`는 STUDENT 전용, `childStudentIds`는 PARENT 전용, 둘 다 TEACHER/ADMIN에는 무의미)를 갖게 된다 — `ADR-008`이 이미 감수하기로 한 부담과 동일하다.
- 승인 권한이 관리자 한 명에서 "활성 선생님 전원"으로 넓어지므로, 오승인(잘못된 학생에게 학부모를 연결하는 등) 리스크의 표면적이 넓어진다.

### Risks

- `GET /api/v1/students/search`가 인증 없이 학생 이름을 검색할 수 있게 하므로, 가입 목적이 아니더라도 누구나 학생 명부를 부분적으로 조회할 수 있는 정보 노출 리스크가 있다. 이번 결정에서는 이를 완전히 차단하는 요구사항이 없으므로, 검색 결과를 `id`+`name`만으로 최소화하는 것 외의 추가 완화책(요청 빈도 제한 등)은 TASK-008에서 명시적으로 범위 밖으로 남긴다 — 운영 중 악용 사례가 발견되면 재검토한다.
- 담당 배정 기준이 없는 상태에서 "아무 활성 선생님"에게 승인 권한을 준 것은, 서로 다른 반의 선생님이 자신과 무관한 학생의 가입을 승인할 수 있다는 뜻이다. 담당 기준이 확정되면 이 범위를 좁히는 재검토가 필요하다.
- 선생님이 실수로 잘못된 학생에게 학부모 계정을 연결하는 것을 막는 추가 검증(예: 학생 측 동의)은 이 ADR에 포함되지 않았다 — `ADR-008`이 이미 인지했던 동일한 리스크가 승인 주체 확대로 인해 커진 것이며, 발생 시 재검토한다.

## Validation

이 결정을 근거로 `TASK-008`을 작성하면서 이 구조와 맞지 않는 요구사항이 발견되면, 코드를 먼저 바꾸지 않고 이 ADR의 갱신 여부를 먼저 논의한다. "담당 선생님/담당 반" 기준이 확정되는 시점에 Decision 3의 승인 범위를 재검토한다.

## Related Documents

- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md` (다대다 관계를 id 행 엔티티로 관리하는 선례)
- `docs/adr/ADR-008-user-student-parent-connection.md` (이 ADR이 확장하는 Decision 4, 재사용하는 연결 필드 구조)
- `docs/USER_ROLES.md` §4 (담당 선생님/담당 반 배정 기준 미정 — 이 ADR의 Decision 3, Risks와 직결)
- `docs/DOMAIN_MODEL.md` §3.1 (User 도메인)
- `docs/tasks/TASK-008-user-signup-and-approval.md` (이 ADR을 구현하는 TASK)
