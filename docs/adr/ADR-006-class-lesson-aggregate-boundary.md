# ADR-006: Class와 Lesson을 별도 Aggregate로 분리하고, 반-학생/반-선생님 배정은 값 컬렉션으로 관리한다

## Status

Accepted (2026-08-10)

이 ADR은 사용자가 세션 중 직접 결정했다(`ADR-002`와 동일한 방식) — 세 가지 질문(Aggregate 경계, 배정 다대다 여부, Lesson 상태값)에 대해 사용자가 그 자리에서 답했다.

## Date

2026-08-10

## Context

- `ARCHITECTURE.md` §8 Deferred Decision #1("Aggregate 내부 경계 세부 확정")은 "Class와 Lesson이 하나의 Aggregate인지 별도 Aggregate인지, Attendance/Homework/LearningRecord가 Lesson 또는 Student 중 어디에 종속되는 Aggregate인지"를 근거 부족으로 보류해왔다.
- `DOMAIN_MODEL.md` §3.3은 "반과 학생, 반과 선생님의 정확한 배정 방식(다대다 여부 등) 및 수업 상태값은 미정"이라고 명시하고 있었다.
- `TASK-002`(Student), `TASK-003`(User + 인증/인가)가 완료되어, 다음 도메인인 반/수업(Class/Lesson)을 구현하려면 이 경계를 확정해야 한다.
- `REQUIREMENTS.md` 3.2는 반과 수업의 CRUD 동사가 다르다는 것을 보여준다: 반은 "생성, 조회, 수정, **비활성화**", 수업은 "생성, 조회, 수정, **취소**". 또한 수업은 "일정, 출결, 숙제, 학습 기록의 기준점"이 되어 향후 도메인들이 `lessonId`를 직접 참조하게 된다.
- `TASK-003`에서 확립된 `User.roles` 패턴(`@ElementCollection` + `@CollectionTable`로 값 컬렉션을 Aggregate가 직접 소유, `ADR-005`가 이를 "다른 Aggregate 참조가 아니므로 연관관계 금지 대상이 아니다"로 공식화)이 다대다 배정에도 적용 가능한 선례로 존재한다.
- `DOMAIN_MODEL.md` §3.2는 "학생은 반, 수업, 출결, 숙제, 학습 기록과 연결되는 중심 엔티티"라고 명시한다 — 즉 Class가 참조하는 "학생"은 `TASK-002`의 `Student` Aggregate(id)를 의미한다. 반면 이 프로젝트에는 별도의 "Teacher" 도메인이 없으므로, "선생님"은 `TASK-003`의 `User` Aggregate 중 `TEACHER` 역할을 가진 사용자를 의미한다 — 학생과 선생님 참조 대상이 서로 다른 Aggregate라는 점에 주의.

## Decision

이번 ADR은 Deferred Decision #1을 **부분적으로** 해소한다 — Class/Lesson 경계와 반-학생/선생님 배정 방식만 다루고, Attendance/Homework/LearningRecord가 Lesson과 Student 중 어디에 종속되는지는 해당 도메인을 실제로 구현하는 시점까지 계속 보류한다.

1. **Class와 Lesson은 별도의 Aggregate로 분리한다.**
   - `Lesson`은 `Class`를 객체로 참조하지 않고 `classId: Long`만 컬럼으로 보유한다(`ARCHITECTURE.md` §6.1 원칙 그대로 적용).
   - 각 Aggregate는 독립적인 트랜잭션 경계를 가진다.
2. **반-학생, 반-선생님 배정은 다대다이며, `Class` Aggregate가 소유하는 값 컬렉션으로 관리한다.**
   - `Class`는 `studentIds: Set<Long>`, `teacherIds: Set<Long>`을 `@ElementCollection`으로 보유한다(`UserJpaEntity.roles`와 동일 패턴).
   - `studentIds`는 `Student`(`TASK-002`) Aggregate의 id를, `teacherIds`는 `User`(`TASK-003`) Aggregate 중 `TEACHER` 역할을 가진 사용자의 id를 참조한다.
   - 배정 자체를 별도 Aggregate(예: `ClassEnrollment`)로 분리하지 않는다 — 배정에 날짜, 상태 등 부가 속성이 필요하다는 요구사항이 현재 없다.
3. **`Lesson`은 상태값으로 `SCHEDULED`, `COMPLETED`, `CANCELLED`를 가진다.**

## Decision Drivers

- Lesson은 반복적으로 계속 생성되는 개별 수업 세션이라 수가 무한정 늘어날 수 있다 — 하나의 Aggregate에 무한정 늘어나는 자식 컬렉션을 두면 트랜잭션/로딩 비용이 계속 커진다.
- 반과 수업의 CRUD 동사가 다르다(비활성화 vs 취소) — 서로 다른 생명주기를 가진다는 신호다.
- 향후 일정/출결/숙제/학습기록 도메인이 `lessonId`를 직접 ID로 참조해야 하므로, Lesson이 Class에 종속되지 않고 독립적으로 조회 가능해야 한다.
- `ADR-005`가 이미 "여러 Aggregate에 걸친 조회는 QueryDSL + ID 기반 JOIN"을 정책으로 확정했으므로, Class와 Lesson을 분리해도 함께 조회해야 하는 경우 이미 준비된 방식으로 처리할 수 있다 — 분리에 따른 조회 비용 문제가 이미 해소되어 있다.
- `User.roles`(`ADR-005`가 인정한 값 컬렉션 패턴)가 다대다 배정에도 그대로 적용 가능하다 — 배정에 부가 속성이 필요하다는 근거가 없는 지금 시점에는 새 Aggregate를 만드는 것이 `AGENTS.md`/`ARCHITECTURE.md`의 "불필요한 추상화 지양" 원칙에 어긋난다.

## Considered Alternatives

### 1. Class와 Lesson을 하나의 Aggregate로 통합

- 설명: `Class`를 Root로 두고 `Lesson`을 내부 Entity로 관리, `Class`를 통해서만 `Lesson`을 변경.
- 기각 사유: Lesson 목록이 무한정 늘어나면 Class 전체를 로딩하는 비용이 계속 커진다. 반/수업의 CRUD 동사 불일치, 그리고 향후 다른 도메인이 `lessonId`만으로 독립 조회해야 하는 요구와 맞지 않는다.

### 2. 반-학생/반-선생님 배정을 별도 `ClassEnrollment`/`ClassAssignment` Aggregate로 분리

- 설명: 배정 자체를 독립된 Aggregate로 만들어 배정일, 배정 상태 등 부가 속성을 담을 수 있게 한다.
- 기각 사유(현재는): 배정에 부가 속성이 필요하다는 요구사항이 아직 없다. 근거 없이 미리 별도 Aggregate를 만드는 것은 과설계다. 실제로 그런 요구가 생기면 `User.roles` 값 컬렉션에서 별도 Aggregate로 마이그레이션한다.

### 3. (채택) 별도 Aggregate + 값 컬렉션 기반 다대다 배정

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 결정.

## Consequences

### Positive

- Lesson이 무한정 늘어나도 Class Aggregate 로딩 비용에 영향을 주지 않는다.
- 향후 일정/출결/숙제/학습기록 도메인이 `lessonId`를 독립적으로 참조/조회할 수 있다.
- 배정 모델이 `User.roles`와 동일한 패턴이라 코드 일관성이 유지된다.

### Negative

- "이 반에 배정된 학생/선생님 전체 조회" 같은 화면성 조회는 Class와 Student/User를 별도로 조회해 조합해야 한다(`ADR-005`의 QueryDSL ID 기반 JOIN으로 처리).

### Risks

- 배정에 날짜, 승인 상태 등 부가 속성이 실제로 필요해지면, 값 컬렉션 구조로는 표현할 수 없어 별도 Aggregate로 마이그레이션이 필요하다.

## Validation

배정(반-학생/반-선생님)에 날짜, 상태 등 부가 속성이 실제로 필요해지는 시점에 별도 Aggregate 도입 여부를 재검토한다.

## Related Documents

- `docs/ARCHITECTURE.md` §8 Deferred Decision #1(이 ADR로 부분 해소 — Class/Lesson 경계와 배정 방식만, Attendance/Homework/LearningRecord 종속 관계는 계속 보류)
- `docs/adr/ADR-005-query-strategy.md`(값 컬렉션과 QueryDSL ID 기반 JOIN 근거)
- `docs/DOMAIN_MODEL.md` §3.3
- `docs/tasks/TASK-005-class-lesson-management.md`
