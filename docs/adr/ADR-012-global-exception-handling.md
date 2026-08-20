# ADR-012: 공통 예외 베이스와 안전한 catch-all로 전역 예외 처리를 확장한다

## Status

Proposed

## Date

2026-08-21

## Context

- `ARCHITECTURE.md` 8장 Deferred Decision #2("예외 및 에러 응답 규격 — 공통 예외 처리 구조, API 에러 응답 포맷")는 `ADR-003`이 "Aggregate Not Found(404)" 케이스만 해소했고, 나머지(검증 실패 세부 포맷, 비즈니스 규칙 충돌, 예상치 못한 예외/500, 인증·인가 예외)는 여전히 보류 중이다.
- `common/presentation/GlobalExceptionHandler.kt`는 현재 `AggregateNotFoundException` 1개 + 도메인별 개별 예외 10개(`DuplicateLoginIdException`, `DuplicateStudentClaimException`, `InvalidSignupRequestException`, `InvalidUserApprovalException`, `InvalidLessonStateTransitionException`, `InvalidTeacherAssignmentException`, `DuplicateAttendanceException`, `UnassignedAttendanceStudentException`, `UnassignedHomeworkStudentException`, `InvalidHomeworkUpdateException`, `InvalidHomeworkRequestException`)를 각각 별도 `@ExceptionHandler` 메서드로 1:1 매핑하고 있다. 도메인이 늘어날 때마다 이 목록도 계속 늘어나는 구조다.
- `GlobalExceptionHandler`에 매핑되지 않은 예외(예상치 못한 예외)는 `ProblemDetail`이 아닌 Spring Boot 기본 에러 응답 포맷으로 나가, 프로젝트 전체의 에러 응답 포맷이 일관되지 않다.
- `ADR-010`(Decision 8, Considered Alternatives #4)은 `GlobalExceptionHandler`에 `@ExceptionHandler(Exception::class)` catch-all을 추가하는 안을 검토했으나 명시적으로 기각했다. 사유는, `ExceptionHandlerExceptionResolver`가 같은 `@ControllerAdvice` 내에서 가장 근접하게 일치하는 핸들러를 선택하는데, 별도 상속 없이 `Exception::class` 핸들러만 추가하면 `MethodArgumentNotValidException` 등 Spring이 기본으로 400 `ProblemDetail`로 변환해주던 예외까지 이 핸들러가 가로채 500으로 응답하는 회귀가 발생하기 때문이다.
- `AuthService`(`login`, `currentUser`)와 `UserService`(권한 검증 지점 다수)는 Spring Security의 `org.springframework.security.authentication.BadCredentialsException`과 `org.springframework.security.access.AccessDeniedException`을 애플리케이션 코드에서 직접 던진다. 이 예외들은 MVC의 `HandlerExceptionResolver` 체인이 처리하지 못하면 서블릿 필터 체인의 `ExceptionTranslationFilter`까지 전파되어, 거기서 `SecurityConfig`에 설정된 `authenticationEntryPoint`/`accessDeniedHandler`로 처리된다. 현재 `authenticationEntryPoint`는 `HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)`(본문 없이 상태 코드만 설정)이고 `accessDeniedHandler`는 별도 설정이 없어 Spring Security 기본 구현이 사용된다 — 둘 다 다른 API가 공통으로 쓰는 `ProblemDetail` JSON 포맷이 아니다.
- 사용자와 범위를 확정한 결과, 이번 결정은 다음을 포함한다: (1) 기존 도메인 예외 10개 + `AggregateNotFoundException`을 공통 상위 클래스로 통합해 `GlobalExceptionHandler`의 반복 핸들러를 축소, (2) 매핑되지 않은 예외에 대한 안전한 500 catch-all 도입, (3) `BadCredentialsException`/`AccessDeniedException`도 `ProblemDetail` JSON으로 통일.

## Decision

1. **공통 예외 베이스 도입**: `common.domain` 패키지에 다음 추상 클래스를 추가한다.

   ```kotlin
   abstract class UmtleCustomException(
       message: String,
       val status: HttpStatus,
   ) : RuntimeException(message)
   ```

   `AggregateNotFoundException`을 `UmtleCustomException(status = HttpStatus.NOT_FOUND)`을 상속하도록 변경하고(생성자 시그니처는 그대로 유지), 나머지 10개 도메인 예외도 `RuntimeException` 대신 `UmtleCustomException`을 상속하도록 변경한다. **기존에 매핑되어 있던 상태 코드는 그대로 보존**하며, 이번 ADR에서 상태 코드 의미를 재검토하지 않는다(예: `DuplicateAttendanceException`이 현재 `BAD_REQUEST`로 매핑되어 있으면 그대로 둔다).

2. **`GlobalExceptionHandler` 재구성**: `RestControllerAdvice`가 `ResponseEntityExceptionHandler`를 상속하도록 변경한다. 이 Spring 기본 클래스는 `MethodArgumentNotValidException`, `HttpMessageNotReadableException`, `MissingServletRequestParameterException`, `NoResourceFoundException` 등 20여 개 내장 MVC 예외에 대해 이미 `ProblemDetail` 기반 처리 메서드를 자체적으로 가지고 있다. 여기에 추가하는 핸들러는 다음 세 가지뿐이다.
   - `@ExceptionHandler(UmtleCustomException::class)` — 도메인 예외 10개 + Not Found를 모두 `exception.status`로 응답하는 핸들러 1개(기존 11개 메서드를 대체).
   - `@ExceptionHandler(AccessDeniedException::class)`, `@ExceptionHandler(AuthenticationException::class)` — 예외를 그대로 재던진다(`throw exception`). Spring Security 예외를 MVC 계층에서 소비하지 않고 `ExceptionTranslationFilter`까지 전파시키기 위함이다(사유는 Decision Drivers 참고).
   - `@ExceptionHandler(Exception::class)` — 위 어디에도 해당하지 않는 예외를 500 `ProblemDetail`로 응답한다. 응답 본문은 `exception.message`를 노출하지 않고 고정 문구("Internal server error")만 담는다. 처리 전에 ERROR 레벨로 스택트레이스와 함께 로깅한다(`RequestLoggingFilter`가 이미 모든 요청에 `traceId`를 MDC로 주입해두므로 로그 라인에 자동으로 포함된다).
3. **Spring Security 예외의 `ProblemDetail` JSON 통일**: `config` 패키지에 `ProblemDetailAuthenticationEntryPoint`(`AuthenticationEntryPoint` 구현, 401)와 `ProblemDetailAccessDeniedHandler`(`AccessDeniedHandler` 구현, 403)를 추가한다. `SecurityConfig.securityFilterChain`의 `exceptionHandling` 블록에서 기존 `HttpStatusEntryPoint(UNAUTHORIZED)`를 `ProblemDetailAuthenticationEntryPoint`로 교체하고 `accessDeniedHandler`를 `ProblemDetailAccessDeniedHandler`로 지정한다. `AuthService`/`UserService`의 기존 코드는 변경하지 않는다 — `BadCredentialsException`/`AccessDeniedException`을 던지는 위치와 무관하게 `ExceptionTranslationFilter`가 항상 가로채므로 자동으로 새 핸들러를 타게 된다.
4. **검증 실패(400) 포맷은 이번 ADR로 새로 정의하지 않는다.** 기존 Spring 기본 동작(`ProblemDetail`)을 그대로 유지하고, 회귀가 없는지 테스트로 확인만 한다.

## Decision Drivers

- **`ADR-010` Decision 8이 우려한 회귀는 실제로 여전히 유효한 위험이지만, 다른 지점에서 발생한다.** `ResponseEntityExceptionHandler`를 상속하면 내장 MVC 예외(`MethodArgumentNotValidException` 등)에 대한 회귀는 피할 수 있다. 그러나 `ExceptionHandlerExceptionResolver`는 MVC 디스패처 내부에서 `ExceptionTranslationFilter`(서블릿 필터, MVC보다 바깥쪽)보다 먼저 실행되므로, `Exception::class` catch-all만 두고 `AccessDeniedException`/`AuthenticationException`을 별도 처리하지 않으면 이 예외들도 "가장 근접한 일치"로 catch-all에 잡혀 500으로 응답되어 버린다 — Decision 3에서 만드는 401/403 JSON 통일이 무력화된다. 그래서 Decision 2에 재던지기 핸들러를 명시적으로 포함했다.
- 사용자가 이번 검토에서 기존 도메인 예외 10개를 공통 베이스로 통합하는 것과 Security 예외 처리까지 포함하는 것을 명시적으로 확정했다.
- `AGENTS.md`/`ARCHITECTURE.md`의 "불필요한 추상화 지양" 원칙에 따라, 새로 추가하는 추상화는 `UmtleCustomException`(상태 코드를 들고 있는 공통 예외) 하나로 제한한다 — 에러 코드 체계, 에러 응답에 필드 목록을 추가하는 등의 확장은 하지 않는다.

## Considered Alternatives

### 1. 현재 상태 유지 (도메인 예외 11개 개별 핸들러 + catch-all 없음)

- 설명: `ADR-010`의 결정을 그대로 유지한다.
- 기각 사유: 도메인이 늘어날수록 `GlobalExceptionHandler`의 반복 핸들러가 계속 늘어나고, 예상치 못한 예외(500)와 Security 예외(401/403)의 응답 포맷이 나머지 API와 계속 불일치한 상태로 남는다.

### 2. `Exception::class` catch-all만 naive하게 추가 (Security 예외 재던지기 없이)

- 설명: `ResponseEntityExceptionHandler` 상속 없이, 혹은 상속하더라도 `AccessDeniedException`/`AuthenticationException` 재던지기 핸들러 없이 `Exception::class` 핸들러만 추가한다.
- 기각 사유: 전자는 `ADR-010`이 우려한 회귀(내장 검증 예외까지 500으로 응답) 그대로 재현된다. 후자(재던지기 없이 상속만)는 Decision Drivers에서 설명한 대로 `AccessDeniedException`이 MVC 계층에서 먼저 소비되어 403이 아닌 500으로 응답되는 새로운 회귀를 만든다.

### 3. 도메인 예외 10개는 그대로 두고 catch-all과 Security 예외 처리만 추가

- 설명: `UmtleCustomException` 공통 베이스 도입 없이, 기존 11개 핸들러는 유지한 채 `Exception::class` catch-all과 Security 예외 처리만 추가.
- 기각 사유 없음(사용자가 검토했으나) — 사용자가 공통 베이스 통합까지 포함하는 쪽을 명시적으로 선택했다. 반복 핸들러가 계속 늘어나는 근본 문제가 해소되지 않는다는 점이 통합을 선택한 이유다.

## Consequences

### Positive

- `GlobalExceptionHandler`의 반복 핸들러가 11개에서 4개(도메인 예외 통합 1개, Security 예외 재던지기 2개, catch-all 1개)로 줄고, 새 도메인 예외를 추가할 때 핸들러를 추가할 필요가 없어진다(exception 클래스가 `UmtleCustomException`을 상속하며 자기 상태 코드를 갖기만 하면 된다).
- 예상치 못한 예외(500)와 인증/인가 실패(401/403) 모두 다른 API와 동일한 `ProblemDetail` JSON 포맷으로 응답된다 — 프론트엔드가 에러 처리를 단일 포맷으로 다룰 수 있다.
- 500 응답이 내부 예외 메시지를 노출하지 않아 정보 노출 위험이 줄어든다.

### Negative

- 도메인 예외 10개 파일과 `AggregateNotFoundException`을 모두 수정해야 한다(상속 대상 변경) — 변경 파일 수가 많다(11개 exception + `GlobalExceptionHandler` + `SecurityConfig` + 신규 클래스 2개).
- `UmtleCustomException`이라는 공통 추상화가 하나 늘어난다.

### Risks

- `ResponseEntityExceptionHandler` 상속과 `Exception::class` catch-all 조합이 실제로 내장 예외 처리를 가로채지 않는지, Security 예외 재던지기가 실제로 401/403 JSON으로 이어지는지는 반드시 통합 테스트로 확인해야 한다(추정이 아닌 검증 필요) — `TASK-012` Test Scenarios 참고.
- 향후 Spring Boot 버전 업그레이드 시 `ExceptionHandlerExceptionResolver`의 매칭 규칙이나 `ResponseEntityExceptionHandler`의 내장 핸들러 목록이 바뀌면 이 설계의 전제가 깨질 수 있다.

## Validation

`TASK-012` 구현 후 통합 테스트로 각 시나리오(도메인 예외, 매핑 안 된 예외, 검증 실패, 인증 실패, 인가 실패, 서비스에서 직접 던지는 `AccessDeniedException`)를 모두 확인한다. 통과하면 Status를 `Accepted`로 전환한다.

## Related Documents

- `docs/ARCHITECTURE.md` 8장 Deferred Decision #2(이 ADR로 해소)
- `docs/adr/ADR-003-common-not-found-error-handling.md` — `AggregateNotFoundException`의 기존 근거, 이번 ADR이 그 구조를 `UmtleCustomException`으로 흡수
- `docs/adr/ADR-010-request-tracing-and-logging.md` — Decision 8(catch-all 미도입)을 amend. `RequestLoggingFilter`의 `traceId`/`X-Trace-Id` 메커니즘은 그대로 재사용
- `docs/tasks/TASK-012-global-exception-handling.md` — 이 ADR을 구현하는 TASK
