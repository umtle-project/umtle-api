# TASK-004: QueryDSL 조회 인프라 도입 및 연관관계 정책 문서화

## Status

Ready

## Purpose

`ADR-005`(JPA 연관관계 배제 + ID 기반 QueryDSL 조회 전략)의 결정을 실제 코드에 적용한다. 현재 코드에는 원칙 위반 사례가 없으므로, 이 작업의 핵심은 (1) 향후 복잡한 조회를 위한 QueryDSL 인프라를 미리 준비하고, (2) 이미 지켜지고 있는 원칙과 유지 예외(`@ElementCollection`/`@EntityGraph`)의 근거를 코드에 명시하는 것이다.

## Background

사용자가 JPA 조회 구조 원칙(연관관계 대신 ID 참조, 단순 조회는 Spring Data JPA, 복잡한 조회는 QueryDSL, DTO 우선, `@ElementCollection`/`@EntityGraph`는 근거 있을 때만 유지)을 제시해 `docs/adr/ADR-005-query-strategy.md`로 정리했다. 코드를 직접 분석한 결과는 다음과 같다:

- JPA 엔티티 간 연관관계(`@OneToMany`/`@ManyToOne`/`@ManyToMany`/`@OneToOne`) 사용처: **0건**. Student, User 두 Aggregate 모두 서로를 참조하지 않으며, 프로젝트는 이미 `ARCHITECTURE.md` §6.1을 지키고 있다.
- `@ElementCollection` 사용처: `UserJpaEntity.roles`(`src/main/kotlin/com/umtle/umtleapi/user/infrastructure/UserJpaEntity.kt`) — `UserRole` 값 집합을 `user_roles` 테이블에 저장. `@CollectionTable`의 `@JoinColumn(name = "user_id")`는 값 컬렉션 소유자 컬럼 매핑이며, 다른 Aggregate 참조가 아닌 User 소유 값 컬렉션이다.
- `@EntityGraph` 사용처: `UserJpaRepository.findById`/`findByLoginId`(`src/main/kotlin/com/umtle/umtleapi/user/infrastructure/UserJpaRepository.kt`) — 위 값 컬렉션의 기본 지연 로딩을 즉시 로딩으로 전환.
- QueryDSL: 미도입(`build.gradle.kts`에 관련 의존성/플러그인 없음).
- 복잡한 조회(여러 Aggregate JOIN, 동적 조건): 현재 없음. `StudentService`/`UserService` 모두 단일 Aggregate 단순 CRUD뿐.

## Related Documents and Requirement IDs

- `docs/ARCHITECTURE.md` §6.1(JPA 연관관계 사용 금지), §8 Deferred Decision #6(복합 조회 정책, 이 작업으로 해소)
- `docs/adr/ADR-005-query-strategy.md`

## Users and Permissions

해당 없음 — 인프라/조회 계층 작업이며 권한 매트릭스에 영향을 주는 API 변경이 없다.

## Preconditions

`TASK-002`(Student 도메인), `TASK-003`(User 도메인 + 인증/인가), `ADR-005`(Proposed 상태로 존재, 이 작업이 실제 적용 사례가 됨)가 존재해야 한다.

## Scope

### 빌드 설정

- `build.gradle.kts`에 QueryDSL 인프라 추가:
  - `kotlin("kapt")` 플러그인 추가(Kotlin 엔티티에 대해 Java 어노테이션 프로세서로 Q-class를 생성하기 위함).
  - `implementation("com.querydsl:querydsl-jpa:5.1.0:jakarta")`, `kapt("com.querydsl:querydsl-apt:5.1.0:jakarta")` 추가 — 정확한 최신 안정 버전과 Spring Boot 4.1.0/Hibernate 7 계열과의 호환성은 구현 시 확인한다(아래 Open Questions 참고).
  - 엔티티는 이미 `kotlin("plugin.jpa")`가 제공하는 `allOpen`(`jakarta.persistence.Entity`, `MappedSuperclass`, `Embeddable`)으로 open 처리되어 있어 QueryDSL 프록시 생성과 호환된다 — 추가 설정 불필요.

### 조회 인프라

- `common/infrastructure/QueryDslConfig.kt` 신규 — `EntityManager`를 주입받아 `JPAQueryFactory`를 `@Bean`으로 등록한다.
- 기존 `StudentJpaRepository`, `UserJpaRepository`는 변경하지 않는다 — 둘 다 단일 Aggregate에 대한 단순 조회뿐이라 `ADR-005`의 "단순 조회는 Spring Data JPA" 원칙상 QueryDSL로 옮길 대상이 아니다.
- 실제 QueryDSL 기반 복잡 조회(JOIN, DTO 프로젝션)는 이 작업에서 추가하지 않는다 — 현재 그런 요구가 없다. 인프라만 준비해 다음 도메인(반/수업 등)에서 바로 사용할 수 있게 한다.

### 기존 코드 문서화 (유지 근거 명시)

- `UserJpaEntity.kt`의 `@ElementCollection`/`@CollectionTable` 위에 주석 추가 — 다른 Aggregate에 대한 참조가 아닌 User 소유 값 컬렉션이라 `ARCHITECTURE.md` §6.1 및 `ADR-005`와 무관하게 유지한다는 설명.
- `UserJpaRepository.kt`의 `@EntityGraph` 위에 주석 추가 — 값 컬렉션의 기본 지연 로딩으로 인한 `LazyInitializationException`을 막기 위함이며, 연관관계 제거와는 무관해 `ADR-005`에 따라 유지 대상이라는 설명.

### 검증

- QueryDSL 배선이 실제로 동작하는지 확인하는 최소 스모크 테스트 `QueryDslSmokeTest`(`src/test/kotlin/com/umtle/umtleapi/common/infrastructure/`) 추가 — 생성된 Q-class로 기존 `students` 테이블에 대해 단순 count 조회 1건을 실행해 Testcontainers 통합 테스트 환경에서 Q-class 생성과 `JPAQueryFactory` 배선을 확인한다. 이 테스트는 "복잡한 조회"의 실사용 예시가 아니라 인프라 배선 검증 목적임을 테스트 이름/주석에 명시한다.

## Out of Scope

- 실제 QueryDSL 기반 복잡 조회(JOIN, 동적 조건, DTO 프로젝션) 구현 — 현재 그런 요구가 없다. 다음 도메인(반/수업 등)에서 실제로 필요해질 때 별도 작업으로 진행한다.
- 기존 `StudentJpaRepository`/`UserJpaRepository`의 QueryDSL 전환.
- `ARCHITECTURE.md` §8의 다른 Deferred Decisions(Aggregate 경계, 트랜잭션 전략 등).
- 스키마 변경 — 이 작업은 조회 계층 인프라만 다루며 테이블 구조를 바꾸지 않는다.

## Functional Scenarios

해당 없음 — 사용자 대면 기능 변경이 아니라 조회 인프라/문서화 작업이다. 검증은 아래 Test Scenarios로 대체한다.

## Business Rules

해당 없음.

## API Changes

없음 — 기존 API 동작에 변화가 없다.

## Domain Impact

없음 — 도메인 로직 변경 없이 Infrastructure 계층(빌드 설정, 조회 인프라, 주석)만 다룬다.

## Database Impact

없음 — 신규 마이그레이션 없음.

## Exception and Error Handling

해당 없음 — 기존 예외 처리 변경 없음.

## Test Scenarios

- `QueryDslSmokeTest`: Q-class 기반 조회가 정상 동작하고 예상 결과(예: 등록된 학생 수)를 반환하는지 확인.
- 회귀 확인: 기존 `StudentApiTests`, `StudentTest`, User 관련 기존 테스트가 변경 없이 통과해야 한다.

## Acceptance Criteria

- `./gradlew build`가 kapt Q-class 생성 단계를 포함해 성공한다.
- `./gradlew test`가 `QueryDslSmokeTest`를 포함해 전부 통과한다.
- `./gradlew spotlessCheck`가 통과한다.
- `UserJpaEntity`/`UserJpaRepository`에 `@ElementCollection`/`@EntityGraph` 유지 근거 주석이 추가되어 있다.
- `docs/ARCHITECTURE.md` §8 Deferred Decision #6이 `ADR-005`로 해소되어 있다(단, `ADR-005`가 `Accepted`로 전환된 이후에 갱신 — `ADR-003`/`ADR-004` 선례를 따른다).

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준. 추가로 이 작업 고유의 항목은 없다.

## Open Questions

- QueryDSL의 정확한 버전과 현재 스택(Spring Boot 4.1.0, Hibernate 7 계열, Kotlin 2.3.21)과의 호환성은 구현 시 직접 확인이 필요하다 — 이 문서에는 `5.1.0`을 잠정 기재했다.
- 스모크 테스트가 아닌 실제 복잡 조회 예시가 필요한지는 다음 도메인(반/수업) 구현 시점에 재논의한다 — 이 작업에서 임의로 추가하지 않는다.

## Related ADRs

- `docs/adr/ADR-005-query-strategy.md`
