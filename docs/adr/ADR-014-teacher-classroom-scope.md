# ADR-014: 반-선생님 배정을 1:N으로 정정하고, 선생님의 출결/숙제/학습 기록 쓰기 권한을 담당 학생으로 제한한다

## Status

Accepted (2026-08-21)

이 ADR은 사용자가 세션 중 직접 결정했다(`ADR-002`, `ADR-006`, `ADR-007`, `ADR-008`, `ADR-013`과 동일한 방식) — "담당 학생/담당 반의 배정 기준을 무엇으로 할지" 질문에서 시작해, 사용자가 그 자리에서 반-선생님 배정 규칙(1:N)과 쓰기 권한 스코핑 범위를 확정했다.

## Date

2026-08-21

## Context

- `TASK-006`(출결), `TASK-007`(숙제), `TASK-013`(학생 관리 고도화), `TASK-014`(학습 기록), `TASK-015`(일정)의 Open Questions에 동일한 미결 사항이 반복해서 등장했다: "담당 학생"/"담당 반"의 배정 기준이 없어 선생님의 조회·쓰기 범위를 시스템적으로 제한하지 못하고, 현재는 `TEACHER` 역할만 가지면 전체 데이터에 접근 가능한 상태다.
- `USER_ROLES.md` §4도 이를 명시적으로 미정 항목으로 남겨두고 있었다: `"담당 학생"/"담당 반"의 배정 기준 — 반-학생, 반-선생님 배정 규칙 자체가 DOMAIN_MODEL.md 6장에 미정으로 남아 있어, 이 기준이 정해지기 전에는 "담당"의 범위를 시스템적으로 판단할 수 없다.`
- `ADR-006`은 반-학생, 반-선생님 배정을 모두 다대다로 결정했고(`Classroom.studentIds: Set<Long>`, `Classroom.teacherIds: Set<Long>`), `class_teachers`/`class_students` 조인 테이블로 구현되어 있다.
- 그러나 실제 운영 규칙은 **반 하나에는 선생님이 최대 1명만 배정된다**(선생님 : 반 = 1 : N, 반 : 학생 = N : M 그대로 유지)는 것이 이번 세션에서 확인됐다. 이는 `ADR-006` Decision 2 중 반-선생님 배정 부분과 배치된다.
- `Attendance`(`ADR-007`), `Homework`(`ADR-007`), `LearningRecord`(`ADR-007`)는 세 도메인 모두 `studentId`를 필수로 참조한다(`Homework`의 `lessonId`는 선택). 현재 `SecurityConfig`는 이 세 도메인의 쓰기 API(`POST`/`PATCH`/`DELETE`)를 `hasRole("TEACHER")`로만 제한하고, 리소스 소유 여부(담당 학생인지)는 검증하지 않는다 — 관리자는 이 세 도메인에 쓰기 권한이 아예 없다(역할 자체가 `TEACHER` 전용).
- 학생/학부모의 "본인/자녀 관련 조회" 스코핑은 `TASK-015` Open Questions에서 별도 작업으로 이미 명시적으로 미뤄져 있으며, 이 ADR도 그 범위를 다루지 않는다.

## Decision

1. **반-선생님 배정을 1:N으로 정정한다 — `ADR-006` Decision 2의 반-선생님 부분을 amend한다.**
   - `Classroom.teacherIds: Set<Long>` → `Classroom.teacherId: Long?`(nullable, 단일 값)로 변경한다.
   - `class_teachers` 조인 테이블을 제거하고, `classes` 테이블에 `teacher_id BIGINT NULL` 컬럼을 추가한다(기존 데이터 이행 포함).
   - 반-학생 배정(`Classroom.studentIds: Set<Long>`, `class_students` 조인 테이블)은 변경하지 않는다 — `ADR-006`의 해당 부분은 그대로 유효하다.
   - `assignTeacher`는 기존에 배정된 선생님이 있으면 교체(덮어쓰기)한다. 명시적으로 해제하려면 `unassignTeacher`(→ `teacherId = null`)를 사용한다.
2. **"담당 반"과 "담당 학생"을 정의한다.**
   - 선생님 `T`의 **담당 반** = `Classroom.teacherId == T.id`인 모든 `Classroom`.
   - 선생님 `T`의 **담당 학생** = 담당 반들의 `studentIds`를 합집합한 집합.
3. **선생님의 조회(GET) 권한은 변경하지 않는다.** 기존처럼 선생님은 `Attendance`/`Homework`/`LearningRecord`/`Schedule`을 전체 대상으로 조회할 수 있다(`TASK-006`/`007`/`014`/`015`가 확립한 정책 유지).
4. **선생님의 `Attendance`/`Homework`/`LearningRecord` 쓰기(생성·수정·삭제) 권한을 담당 학생으로 제한한다.**
   - 세 도메인 모두 레코드가 항상 `studentId`를 가지므로(`ADR-007`), 스코핑 규칙은 도메인에 관계없이 동일하다: **대상 레코드의 `studentId`가 요청한 선생님의 담당 학생 집합에 속하지 않으면 403을 반환한다.**
   - `Attendance.record`, `Homework.assign`(`lessonId` 유무와 무관 — `lessonId`가 있어도 스코핑 판단은 `studentId` 기준으로 통일한다), `LearningRecord.record`: 생성 시 `studentId`로 판단.
   - `Attendance.updateStatus`, `Homework.update`/`delete`, `LearningRecord.update`: 대상 레코드를 조회해 그 `studentId`로 판단.
   - `Schedule`은 이 ADR의 적용 대상이 아니다 — `TASK-015`에 의해 선생님은 이미 `Schedule` 쓰기 권한이 전혀 없다(조회만 가능, 403).
5. **관리자(`ADMIN`)는 영향받지 않는다.** 관리자는 `Attendance`/`Homework`/`LearningRecord` 쓰기 권한이 이 ADR 이전부터 없었고(역할이 `TEACHER` 전용), 이 ADR도 이를 바꾸지 않는다.
6. **학생/학부모의 "본인/자녀 관련" 조회 스코핑은 이번 ADR의 범위 밖이다.** `TASK-015` Open Questions에 남겨진 대로 별도 작업에서 다룬다.
7. **반/수업(`Classroom`/`Lesson`) 자체의 관리 권한(생성·조회·수정·비활성화/취소)은 변경하지 않는다** — 관리자 전용으로 유지한다(`DOMAIN_MODEL.md` §3.3).

## Decision Drivers

- 실제 학원 운영에서 반-선생님은 1:N이 맞다(한 선생님이 여러 반을 맡을 수 있지만, 한 반의 담임은 한 명이다) — 사용자가 세션 중 직접 확인.
- `Attendance`/`Homework`/`LearningRecord` 모두 `studentId`를 필수로 참조하는 공통 구조(`ADR-007`)를 가지고 있어, "담당 학생"이라는 단일 기준으로 세 도메인의 쓰기 스코핑을 동일한 규칙으로 통일할 수 있다 — 도메인마다 다른 스코핑 로직을 만들 근거가 없다.
- 조회는 전체 허용을 유지하고 쓰기만 제한하기로 결정해, 기존 조회 관련 TASK(`006`/`007`/`014`/`015`)의 정책을 깨지 않으면서 가장 리스크가 큰 쓰기 오남용(담당 아닌 학생의 기록을 임의로 생성·수정)만 우선 차단한다.
- `USER_ROLES.md` §4에 5개 TASK에 걸쳐 반복 등장한 미결 사항을 이번 결정으로 해소해, 향후 작업에서 동일 질문을 반복하지 않는다.

## Considered Alternatives

### 1. 반-선생님 배정을 다대다로 유지하고 "담당" 판단만 별도 규칙 추가

- 기각 사유: 사용자가 확인한 실제 운영 규칙(반 1개당 선생님 1명)과 코드 타입(`Set<Long>`)이 불일치한 채로 남아, 향후 "이 반의 선생님은 누구인가"를 항상 `.single()` 같은 방어적 코드로 다뤄야 한다 — 타입이 실제 규칙을 표현하지 못한다.

### 2. 선생님의 조회 권한도 함께 담당 반/학생으로 제한

- 기각 사유(현재는): 사용자가 이번 결정에서 조회는 전체 허용, 쓰기만 제한하기로 범위를 명시적으로 좁혔다. 조회 제한은 학생/학부모 스코핑(`TASK-015` Open Questions)과 함께 별도 작업에서 종합적으로 재검토하는 것이 일관성 있다.

### 3. (채택) 반-선생님 1:N 정정 + 세 도메인 쓰기만 담당 학생으로 제한

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 결정.

## Consequences

### Positive

- `Classroom.teacherId` 타입이 실제 업무 규칙(반 1개당 선생님 1명)을 정확히 표현하게 된다.
- 담당 아닌 학생의 출결/숙제/학습 기록을 임의로 생성·수정·삭제하는 것을 막아, 데이터 무결성과 책임 소재가 명확해진다.
- 세 도메인의 쓰기 스코핑 규칙이 "`studentId` ∈ 담당 학생" 하나로 통일되어 구현·리뷰가 단순하다.
- `USER_ROLES.md` §4의 "담당 학생/담당 반 배정 기준" 미결 항목이 해소되어 표로 이동할 수 있다.

### Negative

- `class_teachers` 조인 테이블 제거 및 `classes.teacher_id` 컬럼 추가 마이그레이션이 필요하다. 기존에 반 하나에 선생님이 2명 이상 배정된 데이터가 있다면 마이그레이션 시 수동 정리가 필요하다(`TASK-017`에서 실제 데이터 확인 후 처리).
- 담당 반이 아직 없는 선생님(신규 배정 전)이나, 어떤 반에도 속하지 않은 학생(담당 학생 집합에 없음)에 대해서는 **어떤 선생님도** 해당 학생의 출결/숙제/학습 기록을 쓸 수 없게 된다 — 관리자도 이 세 도메인에 쓰기 권한이 없으므로, 반 배정이 누락되면 그 학생에 대한 기록 자체가 불가능해진다.

### Risks

- 위 "반 미배정 학생은 아무도 기록을 못 쓴다" 리스크는 이번 ADR에서 완전히 해소하지 않는다 — 실제 운영에서 반 배정 누락이 자주 발생하면, 관리자에게 이 세 도메인의 쓰기 권한(우회 경로)을 부여할지 여부를 별도로 재검토해야 한다.
- "담당 수업" 기준(`Attendance`/`Homework`/`LearningRecord`가 아닌 다른 맥락, 예: 향후 `Lesson` 자체의 수정 권한 등)에는 이 ADR을 자동으로 확대 적용하지 않는다 — 필요해지면 별도로 검토한다.

## Validation

- `TASK-017` 구현 후, 반 미배정 학생에 대한 기록 생성이 실제 운영에서 문제가 되는지 관찰한다. 문제가 되면 관리자 우회 권한 부여 여부를 재검토한다.
- 선생님의 조회 권한 제한이나 학생/학부모 스코핑이 필요해지면 별도 ADR/TASK로 다룬다.

## Related Documents

- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md`(반-선생님 배정 부분을 이 ADR이 amend)
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`(세 도메인 모두 `studentId` 필수 참조 — 이번 스코핑 규칙 통일의 근거)
- `docs/adr/ADR-013-teacher-admin-parity-for-student-parent-domain.md`(동일한 "사용자가 세션 중 직접 결정" 패턴의 선례)
- `docs/USER_ROLES.md` §4(이 ADR로 "담당 학생/담당 반 배정 기준" 항목 해소)
- `docs/tasks/TASK-006-attendance-management.md`, `TASK-007-homework-management.md`, `TASK-013-student-management-enhancement.md`, `TASK-014-learning-record-management.md`, `TASK-015-schedule-management.md`(Open Questions 근거)
- `docs/tasks/TASK-017-teacher-classroom-scope.md`(이 ADR을 구현하는 작업)
