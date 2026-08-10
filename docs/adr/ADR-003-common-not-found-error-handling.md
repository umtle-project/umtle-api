# ADR-003: 공통 "Not Found" 예외 처리만 우선 통일하고, 나머지 에러 응답 규격은 계속 보류한다

## Status

Proposed

이 ADR은 초안이며 아직 구현되지 않았다. 채택 여부는 사람이 결정하며, 채택 시에도 기존 `StudentExceptionHandler`/`StudentNotFoundException`을 이 패턴으로 옮기는 작업은 별도 task로 진행한다.

## Date

2026-08-09

## Context

- `ARCHITECTURE.md` 8장 Deferred Decision #4("예외 및 에러 응답 규격 — 공통 예외 처리 구조, API 에러 응답 포맷")는 아직 확정되지 않았다.
- `TASK-002`에서 Student 도메인을 구현하며 `StudentNotFoundException`(Domain) + `StudentExceptionHandler`(`@RestControllerAdvice(basePackageClasses = [StudentController::class])`, 404 `ProblemDetail`)를 도메인 한정으로 만들었다.
- 다음 도메인(반/수업, 출결, 숙제 등)이 추가되면 "id로 조회했는데 없음 → 404"라는 동일한 패턴이 매 도메인마다 반복될 가능성이 높다. `REQUIREMENTS.md` 3.1~3.7 전체가 각 도메인에 "조회" 기능을 요구하므로, 이 패턴은 거의 확실히 반복된다.
- 반면 검증 실패(400)의 세부 포맷, 비즈니스 규칙 충돌(409 등), 인프라 예외(500)의 공통 처리 방식은 아직 Student 하나만으로는 실제 요구가 드러나지 않아 근거가 부족하다.

## Decision

- **채택되면**: "Aggregate를 id로 찾지 못함 → 404" 케이스만 프로젝트 공통으로 통일한다.
  - `com.umtle.umtleapi.common.domain` 패키지에 공통 예외(예: `abstract class AggregateNotFoundException(aggregateType: String, id: Any)`)를 두고, 각 도메인의 `XxxNotFoundException`이 이를 상속한다.
  - `com.umtle.umtleapi.common.presentation`에 전역 `@RestControllerAdvice` 하나를 두어 `AggregateNotFoundException` → 404 `ProblemDetail`로 일괄 변환한다. 도메인마다 별도 `@RestControllerAdvice`를 만들지 않는다.
- **그 외 에러 유형(검증 세부 포맷, 충돌/409, 인프라 예외/500 등)은 이 ADR로 결정하지 않는다** — `ARCHITECTURE.md` Deferred Decision #4로 계속 남겨둔다.
- 응답 바디 포맷은 Spring 기본 `ProblemDetail`(RFC 7807)을 그대로 사용한다 — 이미 `StudentExceptionHandler`와 Spring의 기본 검증 실패 처리(400)가 모두 `ProblemDetail`을 쓰고 있어, 새로 정하는 것이 아니라 이미 쓰고 있는 것을 명문화하는 수준이다.

## Decision Drivers

- `REQUIREMENTS.md`의 모든 기능 영역이 "조회"를 요구하므로, "조회 실패 → 404"는 도메인 수만큼 반복될 것이 거의 확실하다 — 반복이 확실한 부분만 선제적으로 통일한다.
- 사용자가 이번 검토에서 명시적으로 "임의로 큰 공통 프레임워크를 만들지 말라"고 지시했다 — 아직 패턴이 드러나지 않은 다른 에러 유형까지 지금 설계하는 것은 과설계다.
- `AGENTS.md`/`ARCHITECTURE.md`의 "불필요한 추상화 지양", "확장성을 고려하되 과도한 추상화는 하지 않는다" 원칙.

## Considered Alternatives

### 1. 현재 상태 유지 (도메인마다 개별 Exception + Handler)

- 설명: `StudentExceptionHandler`처럼 도메인마다 `XxxNotFoundException` + 전용 `@RestControllerAdvice`를 계속 만든다.
- 기각 사유: 도메인이 늘어날수록 동일한 404 처리 로직이 반복되고, 응답 포맷이 도메인마다 조금씩 달라질 위험이 있다(예: 메시지 필드명, 상태 코드 세부 사항).

### 2. 지금 전체 공통 예외 프레임워크를 설계

- 설명: 검증 실패, 비즈니스 규칙 충돌, 인프라 예외까지 포함한 전역 에러 코드 체계와 예외 계층을 한 번에 설계한다.
- 기각 사유: 사용자가 명시적으로 배제했고, 도메인이 1개뿐인 지금 시점에는 충돌(409)이나 인프라 예외의 실제 처리 요구가 드러나지 않아 근거가 부족하다(`ARCHITECTURE.md` 1장 "근거가 부족한 항목은 임의로 확정하지 않는다"와 동일한 기준).

### 3. (채택 후보) "Not Found"만 최소 공통화

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이번 ADR의 제안.

## Consequences

### Positive

- 다음 도메인부터 `XxxExceptionHandler` 반복 작성이 없어진다.
- 404 응답 포맷이 프로젝트 전체에서 일관된다.

### Negative

- 공통 예외 계층(`AggregateNotFoundException`)이라는 작은 추상화가 하나 늘어난다.

### Risks

- 두 번째 도메인을 실제로 구현했을 때 이 패턴이 맞지 않을 수 있다(예: 도메인별로 404 응답에 추가 필드가 필요한 경우). 그런 경우 이 ADR을 `Superseded`로 갱신하고 재논의한다.

## Validation

다음 도메인(반/수업 등)을 구현할 때 이 패턴을 실제 적용해보고, 잘 맞으면 `Accepted`로 전환하며 `StudentExceptionHandler`/`StudentNotFoundException`도 함께 마이그레이션한다. 맞지 않으면 이 ADR을 `Superseded`로 표시하고 대안을 다시 논의한다.

## Related Documents

- `docs/ARCHITECTURE.md` 8장 Deferred Decision #4(이 ADR로 일부만 해소 — "Not Found" 케이스 한정, 나머지는 계속 보류)
- `docs/tasks/TASK-002-student-management.md`
