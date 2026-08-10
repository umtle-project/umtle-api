# ADR-005: JPA 연관관계를 배제하고 ID 기반 QueryDSL 조회 전략을 채택한다

## Status

Accepted

`TASK-004`에서 QueryDSL 조회 인프라와 유지 예외 주석을 실제 코드에 적용했다.

## Date

2026-08-10

## Context

- `ARCHITECTURE.md` §6.1("JPA 연관관계 사용 금지")은 `@ManyToOne`/`@OneToMany`/`@ManyToMany`/`@JoinColumn` 등 연관관계 매핑을 쓰지 않고 다른 Aggregate는 식별자 값으로만 참조하도록 이미 원칙으로 정해두었다.
- `ARCHITECTURE.md` §8 Deferred Decision #6("복합 조회(Read Model) 정책")은 "여러 Aggregate에 걸친 화면성 조회가 늘어나면 Application Service가 여러 Repository를 조합하느라 비대해질 수 있다"는 문제를 인지하면서도, 별도의 조회 전용 Read/Query Adapter를 허용할지·어느 계층에 둘지는 "실제로 그런 복합 조회 요구가 나타나는 시점에 구체적 사례를 근거로 결정한다"며 보류해왔다.
- 현재 코드(Student, User 두 Aggregate)를 직접 확인한 결과, `@OneToMany`/`@ManyToOne`/`@ManyToMany`/`@OneToOne` 등 JPA 엔티티 간 연관관계 사용처는 0건이다 — 즉 프로젝트는 이미 §6.1 원칙을 그대로 지키고 있다. Student와 User는 서로를 참조하지도 않는다.
- `User.roles`처럼 도메인 모델이 값 컬렉션으로 표현하는 데이터도 JPA `@ElementCollection`으로 매핑하지 않고, `id + ownerId + value`만 가진 단순 JPA Entity 테이블로 저장한다. 이 방식은 `@JoinColumn`, 복합 PK, 물리 FK 없이도 도메인에는 `Set<UserRole>` 같은 순수 컬렉션을 유지할 수 있게 한다.
- 현재 두 Aggregate 모두 단일 Aggregate에 대한 단순 CRUD 조회만 존재하며, 여러 Aggregate에 걸친 JOIN이나 동적 조건 조회는 아직 실제로 필요해진 적이 없다. 사용자가 이번 ADR로 Deferred Decision #6을 미리 확정하기로 결정했다 — 다음 도메인(반/수업 등)이 추가되면 곧바로 필요해질 것으로 예상되는 정책이기 때문이다.
- 프로젝트에 QueryDSL은 아직 도입되어 있지 않다(`build.gradle.kts`에 관련 의존성/플러그인 없음, 생성된 `Q*` 클래스 없음).

## Decision

- JPA 엔티티 간 연관관계(`@OneToMany`, `@ManyToOne`, `@ManyToMany`, 연관관계용 `@JoinColumn` 등)는 사용하지 않는다 — 이미 지켜지고 있는 §6.1 원칙을 프로젝트의 영구 결정으로 재확인한다.
- 데이터베이스 물리 외래 키 제약도 사용하지 않는다. 참조 대상 존재 여부는 Application Service가 Repository 조회로 검증하고, 조회 조합은 QueryDSL/JPQL의 ID 기반 명시 조인 또는 Application 레벨 조합으로 처리한다.
- 도메인 값 컬렉션을 저장하는 보조 테이블도 JPA `@ElementCollection`/`@CollectionTable` 대신 `id`를 가진 단순 JPA Entity로 매핑한다. 보조 테이블은 복합 PK 대신 단일 `id` PK를 사용하고, 필요한 중복 방지는 unique 제약으로 표현한다.
- 단, **Aggregate 내부 생명주기 관리 등 명확한 도메인적 이유가 있는 경우에 한해** 예외를 허용할 수 있다. 예외를 두는 경우 그 이유를 코드 주석과 관련 ADR/TASK 문서에 명시한다. 이 ADR 시점에는 그런 예외 사례가 없다.
- 단순 조회(단일 Aggregate, 단일 조건 위주의 조회)는 Spring Data JPA의 파생 쿼리(method name query) 또는 `@Query`(JPQL)를 사용한다.
- JOIN, 동적 조건, DTO 프로젝션 등 복잡한 조회는 QueryDSL(`JPAQueryFactory`)을 우선 사용한다. QueryDSL로 여러 Aggregate를 함께 조회할 때 JOIN은 JPA 연관관계가 아니라 ID 컬럼을 기준으로 명시적으로 작성한다:

  ```kotlin
  queryFactory
      .select(...)
      .from(enrollment)
      .join(student).on(enrollment.studentId.eq(student.id))
      .where(...)
  ```

- 복잡한 조회는 가능하면 Entity가 아닌 조회 전용 DTO(`Projections.constructor(...)` 등)로 반환한다 — Entity를 그대로 반환하면 Presentation 계층이 다시 암묵적으로 지연 로딩된 필드를 건드릴 위험이 생긴다.
- `@EntityGraph`는 JPA 연관관계나 `@ElementCollection` 기반 로딩 제어가 아니라면 사용하지 않는다. 필요한 조회 최적화는 QueryDSL/JPQL 또는 별도 Repository 메서드로 명시한다.

## Decision Drivers

- `ARCHITECTURE.md` §6.1이 이미 명시한 도입 이유(Aggregate 경계를 코드 수준에서 강제, 지연 로딩/N+1 등 성능 문제 사전 차단, 트랜잭션 범위와 변경 영향 범위 명확화, 도메인 간 결합도를 낮춰 향후 모듈 분리 가능성 유지)가 그대로 이 결정의 근거가 된다.
- 현재 코드가 이미 이 방향과 정확히 일치한다 — 새로 무언가를 금지하는 것이 아니라 이미 지켜지고 있는 관행을 공식 결정으로 승격시키는 것이라 채택에 따른 추가 리스크가 낮다.
- `ARCHITECTURE.md` §8 Deferred Decision #6이 예견한 "여러 Aggregate에 걸친 조회가 늘어나면 Application Service가 비대해질 위험"은 다음 도메인(반/수업, 출결 등)이 추가되면 곧 현실화될 것으로 예상된다 — 그 시점에 임기응변으로 결정하기보다 지금 원칙을 정해두면 일관성을 유지할 수 있다.

## Considered Alternatives

### 1. JPA 연관관계 + `@EntityGraph`로 복잡한 조회 해결

- 설명: 여러 Aggregate에 걸친 조회가 필요할 때 `@ManyToOne`/`@OneToMany` 연관관계를 추가하고 `@EntityGraph`로 즉시 로딩을 제어한다.
- 기각 사유: `ARCHITECTURE.md` §6.1의 기존 원칙과 정면으로 충돌한다. 연관관계를 한 번 허용하면 암묵적인 엔티티 그래프 탐색이 다시 가능해져, Aggregate 경계를 코드 수준에서 강제한다는 이 프로젝트의 핵심 설계 원칙이 무너진다.

### 2. 모든 복잡한 조회를 Application Service의 다중 Repository 조합으로 처리

- 설명: JOIN이 필요한 경우에도 각 Aggregate의 Repository를 개별 호출한 뒤 Application Service(메모리)에서 조합한다 — 현재 §6.1이 이미 서술하고 있는 기본 방식을 복잡한 조회에도 예외 없이 계속 적용.
- 기각 사유: 단순 조합(예: id로 개별 조회 후 합치기)에는 적합하지만, 실제 JOIN이나 동적 조건이 필요한 조회(목록 필터링, 페이징과 결합된 여러 Aggregate 요약 등)에서는 DB가 처리할 수 있는 일을 애플리케이션 메모리로 옮기게 되어 N+1과 성능 문제를 오히려 재도입한다.

### 3. (채택) QueryDSL + ID 기반 명시적 JOIN + DTO 우선

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 제안.

## Consequences

### Positive

- 도메인이 늘어나도(반/수업, 출결, 숙제 등) 조회 전략이 일관되게 유지된다.
- Aggregate 경계가 JPA 연관관계 부재로 계속 코드 수준에서 강제된다.
- 복잡한 조회가 DTO로 반환되어 Presentation 계층이 Entity의 지연 로딩된 필드를 암묵적으로 건드릴 위험이 줄어든다.

### Negative

- QueryDSL을 쓰려면 빌드 설정(Kotlin `kapt` 플러그인, Q-class 생성)이 추가되어 빌드 복잡성이 늘어난다.

### Risks

- 이 ADR 시점에는 실제로 QueryDSL을 사용하는 복잡한 조회 사례가 없어, 인프라 도입 자체는 스모크 테스트 수준으로만 검증 가능하다. 첫 실제 복잡 조회(다음 도메인 구현 시)에서 이 정책이 실전에 잘 맞는지 별도로 검증해야 한다.

## Validation

반/수업 등 다음 도메인에서 실제로 여러 Aggregate에 걸친 JOIN 조회가 필요해지는 시점에, 이 ADR이 제안한 QueryDSL + ID 기반 JOIN 방식이 실제 요구에 잘 맞는지 재검증한다. 맞지 않으면 이 ADR을 `Superseded`로 표시하고 대안을 재논의한다.

## Related Documents

- `docs/ARCHITECTURE.md` §6.1(JPA 연관관계 사용 금지, 이 ADR과 일치하는 기존 원칙)
- `docs/ARCHITECTURE.md` §8 Deferred Decision #6(복합 조회 정책, 이 ADR로 해소)
- `docs/tasks/TASK-004-querydsl-setup.md`
