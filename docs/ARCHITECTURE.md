# ARCHITECTURE.md

# 움틀 아키텍처 설계

이 문서는 움틀(Umtle) 백엔드 API의 기술 구조와 구현 원칙을 정의한다.

이 문서는 `PRD.md`, `REQUIREMENTS.md`, `DOMAIN_MODEL.md`의 하위 문서이며, 상위 문서와 모순되지 않는 범위에서만 유효하다. 문서 우선순위는 `AGENTS.md`를 따른다.

`USER_ROLES.md`는 도메인별 권한 매트릭스를 정의하지만, 인증/인가의 구체적인 구현 방식 등 권한 관련 기술 결정은 이 문서에서 확정하지 않고 8장 "Deferred Decisions"로 분리한다.

---

## 1. 목적 및 범위

- 움틀 백엔드가 따르는 아키텍처 스타일과 계층 구조를 정의한다.
- 도메인 모델을 프레임워크와 영속성 관심사로부터 보호하기 위한 구조적 원칙을 정의한다.
- 데이터베이스 스키마, API 명세, 코드 구현은 이 문서의 범위가 아니다.
- 이 문서는 초안이며, 근거가 부족한 항목은 임의로 확정하지 않고 명시적으로 보류한다.

---

## 2. 아키텍처 스타일

- 움틀은 초기 단계에서 **단일 모놀리스(Single Monolith)**로 개발한다.
- 하나의 Gradle 프로젝트, 하나의 배포 단위로 구성한다.
- 현재 시점에는 다음 기술을 도입할 근거가 없으므로 명시적으로 범위에서 제외한다.
  - MSA (마이크로서비스 아키텍처)
  - Redis
  - Kafka
  - CQRS
- 위 항목은 서비스 규모, 트래픽, 조직 구조 등 도입을 정당화할 근거가 생겼을 때 별도 논의와 `docs/adr/`를 통해 재검토한다.

---

## 3. 레이어 구조 (DDD 4-Layered Architecture)

움틀은 다음 4개 계층으로 구성된 DDD Layered Architecture를 따른다.

```
Presentation → Application → Domain ← Infrastructure
```

- 의존 방향은 항상 바깥 계층에서 안쪽(Domain)을 향한다.
- Domain 계층은 다른 어떤 계층에도 의존하지 않는다.
- Infrastructure 계층은 Domain 계층이 정의한 인터페이스(포트)를 구현한다.

### 3.1 Presentation Layer

- 책임: HTTP 요청/응답 처리, 입력값의 형식적 검증, Application Service 호출.
- 구성 요소: Controller, Request/Response DTO.
- 인증/인가는 프레임워크(Spring Security)에 위임하며, 상세 정책은 `USER_ROLES.md` 확정 후 별도로 다룬다.
- 도메인 규칙을 포함하지 않는다.

### 3.2 Application Layer

- 책임: 유스케이스 단위의 흐름 조합, **트랜잭션 경계 관리**, Domain Repository(포트) 호출.
- 구성 요소: Application Service.
- Application Service는 하나의 유스케이스를 완결시키기 위해 필요한 Domain 객체와 Repository를 조합한다.
- DTO와 도메인 모델 간 변환을 조정한다 (변환 로직 자체는 Domain 또는 별도 매퍼가 담당할 수 있다).
- 트랜잭션 관리 원칙은 5장에서 별도로 정의한다.

### 3.3 Domain Layer

- 책임: 핵심 비즈니스 규칙과 불변식 표현.
- 구성 요소: Aggregate, Entity, Value Object, Domain Service, Repository 인터페이스(포트).
- Domain 계층의 클래스는 Spring이나 JPA 등 프레임워크 어노테이션에 의존하지 않는다.
- `DOMAIN_MODEL.md`에서 정의한 도메인 개념과 유비쿼터스 언어를 그대로 반영한다.

### 3.4 Infrastructure Layer

- 책임: 영속성 및 외부 연동 구현.
- 구성 요소: JPA Entity, Spring Data JPA Repository 구현체(어댑터), 도메인 모델 ↔ JPA Entity 매퍼.
- Domain 계층이 정의한 Repository 인터페이스를 구현한다.
- Presentation, Application, Domain 계층은 JPA Entity의 존재를 알지 못한다.

---

## 4. 도메인 모델과 JPA Entity 분리

- 도메인 모델(Domain Model)과 JPA Entity는 서로 다른 클래스로 분리한다.
- 도메인 모델은 Domain 계층에 위치하며, 비즈니스 규칙과 불변식을 표현하는 순수 객체다.
- JPA Entity는 Infrastructure 계층에 위치하며, 영속성 매핑만을 책임진다.
- 두 모델 간 변환은 Infrastructure 계층(Repository 구현체 또는 매퍼)이 담당하며, 이 변환 로직 밖으로 JPA Entity가 노출되지 않는다.
- 분리 목적:
  - 도메인 로직이 프레임워크나 영속성 기술 변경에 영향받지 않도록 한다.
  - 도메인 모델이 표현하는 비즈니스 규칙과 영속성 관심사(컬럼, 매핑, 인덱스 등)를 명확히 분리한다.
  - 도메인 모델을 테스트할 때 데이터베이스나 JPA 컨텍스트에 의존하지 않도록 한다.

패키지 배치는 계층별로 구분하며(예: 도메인 모델은 domain 패키지, JPA Entity는 infrastructure 하위 패키지), 구체적인 패키지 트리와 명명 규칙은 구현 시점에 결정한다.

---

## 5. Aggregate 설계 원칙

`DOMAIN_MODEL.md`에서 정의한 8개 도메인은 각각 하나 이상의 Aggregate 후보를 가진다.

| 도메인 | Aggregate 후보 |
|--------|-----------------|
| 사용자 | User |
| 학생 | Student |
| 반/수업 | Class, Lesson |
| 일정 | Schedule |
| 출결 | Attendance |
| 숙제 | Homework |
| 학습 기록 | LearningRecord |
| 공지 | Notice |

원칙:

- 모든 상태 변경은 Aggregate Root를 통해서만 이루어진다.
- **Aggregate 간에는 식별자(ID)로만 참조하며, 다른 Aggregate에 대한 객체 참조를 갖지 않는다.**
  - 예: `Lesson`이 `Class`를 참조할 때 `Class` 객체가 아닌 `classId` 값만 보유한다.
- 하나의 Aggregate는 하나의 트랜잭션 안에서 일관성을 보장하는 단위다.
- Class와 Lesson이 하나의 Aggregate인지, 별도의 Aggregate인지 등 세부 경계는 아직 확정할 근거가 없어 7장 "Deferred Decisions"로 분리한다.

---

## 6. JPA 연관관계 및 트랜잭션 원칙

### 6.1 JPA 연관관계 사용 금지

- `@ManyToOne`, `@OneToMany`, `@ManyToMany`, `@JoinColumn` 등 JPA 연관관계 매핑을 사용하지 않는다.
- JPA Entity는 다른 Aggregate를 참조할 때 연관관계 대신 식별자 값(예: `Long classId`)만 컬럼으로 보유한다.
- 여러 Aggregate의 데이터를 함께 조회해야 하는 경우, 각 Aggregate의 Repository를 통해 개별 조회한 뒤 Application Service에서 조합한다.
- 도입 이유:
  - Aggregate 경계를 코드 수준에서 강제하여 의도치 않은 그래프 탐색을 방지한다.
  - 지연 로딩(N+1), 불필요한 즉시 로딩 등 성능 문제를 사전에 차단한다.
  - 트랜잭션 범위와 변경 영향 범위를 명확하게 유지한다.
  - 도메인 간 결합도를 낮춰 향후 모듈 분리 가능성을 열어둔다.

### 6.2 트랜잭션 관리

- 트랜잭션 경계는 **Application Service**에서만 관리한다 (`@Transactional`은 Application Service 메서드에 위치한다).
- Domain 계층과 Infrastructure 계층은 트랜잭션 어노테이션을 갖지 않는다.
- 하나의 트랜잭션은 원칙적으로 하나의 Aggregate 변경을 단위로 한다.
- 여러 Aggregate를 하나의 요청 안에서 함께 변경해야 하는 경우의 처리 전략(단일 트랜잭션 허용 여부, 도메인 이벤트 도입 여부 등)은 아직 확정할 근거가 없어 7장 "Deferred Decisions"로 분리한다.

---

## 7. 기술 스택

현재 `build.gradle.kts`에 반영된 의존성을 기준으로 한다.

- 언어/프레임워크: Kotlin, Spring Boot (Web MVC)
- 영속성: Spring Data JPA, MySQL
- 마이그레이션: Flyway
- 인증/인가 기반: Spring Security (세부 정책은 미정)
- 검증: Spring Validation
- 식별자: TSID(`com.github.f4b6a3:tsid-creator`)를 `Long`(BIGINT)으로 저장. Domain 계층의 factory 메서드가 Aggregate 생성 시점에 직접 할당하며, Hibernate의 ID 생성 전략에 위임하지 않는다 (`docs/adr/ADR-002-tsid-as-identifier.md`).

다음 기술은 현재 범위에서 제외하며, 구현되지 않은 상태에서 임의로 도입하지 않는다.

- MSA
- Redis
- Kafka
- CQRS

---

## 8. Deferred Decisions

다음 항목은 이번 초안에서 확정할 근거가 부족하여 보류한다. 결정이 필요해지는 시점에 관련 컨텍스트와 함께 별도로 논의하고, 확정되면 `docs/DECISIONS.md` 또는 `docs/adr/`에 기록한 뒤 이 문서를 갱신한다.

1. **Aggregate 내부 경계 세부 확정** — Class와 Lesson이 하나의 Aggregate인지 별도 Aggregate인지, Attendance/Homework/LearningRecord가 Lesson 또는 Student 중 어디에 종속되는 Aggregate인지.
2. **여러 Aggregate 동시 변경 시 정합성 처리 전략** — 단일 트랜잭션 허용 범위, 도메인 이벤트 도입 여부와 시점.
3. **인증/인가 구현 방식** — 세션 기반/토큰 기반 여부, 인가 처리 위치(Presentation vs Application). `USER_ROLES.md` 확정 후 결정.
4. **예외 및 에러 응답 규격** — 공통 예외 처리 구조, API 에러 응답 포맷.
5. **Presentation과 Domain 간 검증(Validation) 책임 분리 기준** — 형식 검증과 비즈니스 규칙 검증의 경계.
6. **모놀리스 내부 패키지/모듈 분리 수준** — 도메인별 패키지 분리 규칙과 향후 모듈 분리(멀티모듈, MSA 전환 등) 판단 기준.
7. **복합 조회(Read Model) 정책** — 5장의 "Aggregate 간 ID 참조" 원칙과 6장의 "JPA 연관관계 사용 금지" 원칙은 유지하되, 여러 Aggregate에 걸친 화면성 조회(예: 학생 목록 + 반/선생님/출결 요약)가 늘어나면 Application Service가 여러 Repository를 직접 조합하느라 비대해질 수 있다. 이 경우 Aggregate 경계나 JPA 연관관계 금지 원칙을 깨지 않는 범위에서 별도의 조회 전용 Read/Query Adapter를 허용할지, 허용한다면 어느 계층에 둘지는 아직 결정하지 않았다. 실제로 그런 복합 조회 요구가 나타나는 시점에 구체적 사례를 근거로 결정한다.

---

## 9. 변경 원칙

- 이 문서의 원칙과 상충하는 구현은 허용하지 않는다.
- 구현 중 이 문서와 다른 접근이 필요하다고 판단되면, 코드를 먼저 변경하지 않고 이 문서의 갱신 여부를 먼저 논의한다.
- Deferred Decisions 항목이 확정되면 해당 섹션을 본문으로 옮기고 목록에서 제거한다.
