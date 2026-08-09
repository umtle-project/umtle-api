# ADR-002: Aggregate 식별자로 TSID를 사용한다

## Status

Accepted (2026-08-09)

## Date

2026-08-09

## Context

- `ARCHITECTURE.md` 8장 Deferred Decision #4는 "식별자(ID) 타입 정책 — Auto-increment 정수 vs UUID 등"을 근거 부족으로 보류해왔다.
- `TASK-002`(학생 관리 도메인 구현)에서 최초의 Aggregate(Student)와 최초의 Flyway 마이그레이션을 작성하게 되면서, 더 이상 보류할 수 없는 시점에 도달했다. 이 결정은 Student뿐 아니라 향후 추가되는 모든 Aggregate(Class, Lesson, Attendance, Homework, LearningRecord, Notice 등)에 동일하게 적용되는 프로젝트 전역 결정이다.
- 사용자가 세션 중 TSID(Time-Sorted Unique Identifier)를 제안했다.
- 프로젝트에 이미 포함된 `org.hibernate.orm:hibernate-core:7.4.1.Final`의 클래스 목록을 직접 확인한 결과, Hibernate 자체에는 `@Tsid` 같은 네이티브 TSID 생성 지원이 없다(`@UuidGenerator` 등 UUID 관련 제너레이터만 존재). 즉 TSID를 쓰려면 별도 라이브러리가 필요하다.

## Decision

- 모든 Aggregate Root의 식별자 타입은 **TSID를 `Long`(64비트)으로 저장**한다.
- TSID 생성은 경량 라이브러리 `com.github.f4b6a3:tsid-creator:5.2.6`(의존성 없음, 유지보수 모드로 API가 안정적)를 사용해 `TsidCreator.getTsid().toLong()`으로 수행한다.
- ID는 Hibernate의 식별자 생성 전략(`@GeneratedValue`)에 위임하지 않고, **Domain 계층의 factory 메서드**(예: `Student.register(...)`)가 Aggregate를 생성하는 시점에 직접 할당한다("assigned identifier"). JPA Entity의 `@Id` 필드는 생성 전략 없이 이미 할당된 값을 그대로 저장한다.
- 이 방식을 선택한 이유는 특정 Hibernate 버전의 ID 생성 통합 기능에 의존하지 않아, 향후 Hibernate/Spring Boot 버전이 바뀌어도 영향을 받지 않기 때문이다.

## Decision Drivers

- **DB 인덱스 성능**: TSID는 시간순으로 정렬되는 64비트 값이라 MySQL(InnoDB) 클러스터드 인덱스에 순차 삽입되어, 완전 랜덤인 UUIDv4 대비 페이지 분할·단편화 문제가 없다.
- **저장 공간과 조인 비용**: `BIGINT`(8바이트)는 문자열 UUID(가변, 보통 36바이트) 대비 인덱스/조인 비용이 낮다. `ARCHITECTURE.md` 6장의 "Aggregate 간 ID 참조" 원칙상 다른 Aggregate가 이 ID를 컬럼으로 계속 들고 다니므로 크기가 누적 비용에 영향을 준다.
- **외부 노출 시 예측 불가능성**: 순수 auto-increment 정수는 순차적이라 전체 학생 수·가입 순서 등이 API 응답에서 유추될 수 있다. TSID는 타임스탬프+노드+카운터 조합이라 auto-increment보다는 추측하기 어렵다(단, 타임스탬프를 인코딩하므로 암호학적으로 안전한 난수는 아니다 — 아래 부정적 결과 참고).
- **Hibernate 버전 비의존성**: Domain factory에서 직접 할당하는 방식은 Hibernate의 ID 생성 통합 기능(버전별로 API가 계속 바뀌는 영역)에 결합되지 않는다.

## Considered Alternatives

### 1. Auto-increment 정수 (`BIGINT AUTO_INCREMENT`)

- 설명: MySQL이 기본 제공하는 순차 정수 기본키.
- 기각 사유: 구현은 가장 단순하지만, 순차값이 API 응답에 그대로 노출되면 가입 순서·전체 건수 등이 추측 가능하다. 사용자가 TSID를 명시적으로 선호했고, TSID도 구현 난이도가 크게 높지 않아 이 대안은 채택하지 않았다.

### 2. 표준 UUID (RFC 4122, 예: UUIDv4)

- 설명: `java.util.UUID.randomUUID()` 또는 Hibernate `@UuidGenerator`로 생성하는 128비트 전역 고유 식별자.
- 기각 사유: 완전 랜덤이라 정렬 순서가 없어 InnoDB 클러스터드 인덱스에 랜덤 삽입되며, `CHAR(36)` 저장 시 `BIGINT` 대비 저장/인덱스 비용이 크다. 이 프로젝트는 아직 분산/다중 DB 환경(`ADR-001` 단일 모놀리스)이 아니라 UUID의 핵심 장점(완전 분산 생성)이 당장 필요하지 않다.

### 3. Hibernate 네이티브 TSID 어노테이션

- 설명: Hibernate가 `@Tsid` 같은 어노테이션을 기본 제공한다면 별도 라이브러리 없이 쓸 수 있다.
- 기각 사유: 실제로 존재하지 않음을 확인했다(`hibernate-core-7.4.1.Final.jar` 내부에 `Tsid` 관련 클래스가 없음, `UuidGenerator`만 존재). 가정으로 남겨두지 않고 직접 확인 후 기각했다.

## Consequences

### Positive

- 모든 향후 Aggregate가 동일한 식별자 정책을 따르므로, 이번에 다시 논의할 필요가 없다.
- `BIGINT` 기반이라 다른 Aggregate가 ID로만 참조하는 `ARCHITECTURE.md` 6장 원칙과 저장/조인 비용 면에서 잘 맞는다.
- Hibernate ID 생성 전략에 의존하지 않아 프레임워크 버전 업그레이드에 영향을 덜 받는다.

### Negative

- `tsid-creator`라는 새 3rd-party 의존성이 추가된다(단, 의존성 없는 경량 라이브러리).
- TSID는 타임스탬프를 인코딩하므로, ID로부터 생성 시각을 역산할 수 있다 — 생성 시각을 감춰야 하는 요구사항이 생기면 재검토가 필요하다(현재 그런 요구사항 없음).

### Risks

- 기본 설정은 노드 ID가 지정되지 않으면 JVM 인스턴스마다 무작위로 선택된다(10비트, 최대 1024개). 현재는 단일 모놀리스·단일 인스턴스(`ADR-001`) 기준이라 충돌 위험이 낮지만, 향후 다중 인스턴스로 수평 확장할 경우 노드 ID를 명시적으로 설정해야 한다 — 그 시점에 별도로 재검토한다.

## Validation

수평 확장(다중 애플리케이션 인스턴스 동시 기동)이 실제로 필요해지는 시점에 노드 ID 설정 방식을 재검토한다. 별도 트리거 없이 별다른 문제가 없는 한 재검토하지 않는다.

## Related Documents

- `docs/ARCHITECTURE.md` 8장 Deferred Decision #4(이 ADR로 해소, 문서에서 제거)
- `docs/tasks/TASK-002-student-management.md`
