# TASK-012: 전역 예외 처리 확장 (공통 예외 베이스 + catch-all + Security 예외 통일)

## Status

Draft

## Purpose

`GlobalExceptionHandler`의 반복되는 도메인 예외 핸들러를 공통 베이스로 통합하고, 매핑되지 않은 예외(500)와 Spring Security 예외(401/403)까지 프로젝트 전체가 쓰는 `ProblemDetail` JSON 포맷으로 통일한다.

## Background

`common/presentation/GlobalExceptionHandler.kt`는 도메인 예외 11개를 각각 별도 `@ExceptionHandler` 메서드로 매핑하고 있어, 도메인이 늘어날 때마다 핸들러도 계속 늘어나는 구조다. 또한 매핑되지 않은 예외(500)는 `ProblemDetail`이 아닌 Spring Boot 기본 에러 포맷으로 응답되고, `AuthService`/`UserService`가 던지는 Spring Security의 `BadCredentialsException`/`AccessDeniedException`도 다른 API와 다른 응답 포맷을 반환한다. `ADR-003`과 `ADR-010`이 이 공백을 각각 일부만 해소하고 나머지는 `ARCHITECTURE.md` §8 Deferred Decision #2로 보류해뒀다. 이 TASK는 `ADR-012`가 정한 결정을 구현한다.

## Related Documents and Requirement IDs

- `docs/adr/ADR-012-global-exception-handling.md` — 이 TASK가 구현하는 결정
- `docs/adr/ADR-003-common-not-found-error-handling.md` — `AggregateNotFoundException`의 기존 근거, 이번에 `UmtleCustomException`으로 흡수
- `docs/adr/ADR-010-request-tracing-and-logging.md` — Decision 8(catch-all 미도입)을 이 TASK가 amend. `traceId`/`X-Trace-Id` 메커니즘은 그대로 재사용(변경 없음)
- `docs/ARCHITECTURE.md` §8 Deferred Decision #2

## Users and Permissions

해당 없음 — 특정 역할에 대한 권한 규칙을 바꾸지 않는다. 다만 인증/인가 실패(401/403) 시 "누가 어떤 요청을 했는지"와 무관하게 모든 역할에 대해 응답 본문 포맷이 바뀐다(상태 코드는 동일하게 유지).

## Preconditions

- `ADR-012`가 `Accepted` 상태로 전환되어 있어야 한다.

## Scope

- `common.domain`에 `UmtleCustomException(message: String, val status: HttpStatus) : RuntimeException(message)` 추상 클래스 신설.
- `AggregateNotFoundException`을 `UmtleCustomException`(status = `NOT_FOUND`) 상속으로 변경(생성자 시그니처는 그대로 유지).
- 다음 10개 도메인 예외를 `RuntimeException` 대신 `UmtleCustomException`(기존과 동일한 상태 코드)을 상속하도록 변경:
  - `DuplicateLoginIdException`, `DuplicateStudentClaimException` → `CONFLICT`
  - `InvalidSignupRequestException` → `BAD_REQUEST`
  - `InvalidUserApprovalException` → `CONFLICT`
  - `InvalidLessonStateTransitionException` → `BAD_REQUEST`
  - `InvalidTeacherAssignmentException` → `BAD_REQUEST`
  - `DuplicateAttendanceException` → `BAD_REQUEST`
  - `UnassignedAttendanceStudentException` → `BAD_REQUEST`
  - `UnassignedHomeworkStudentException` → `BAD_REQUEST`
  - `InvalidHomeworkUpdateException` → `BAD_REQUEST`
  - `InvalidHomeworkRequestException` → `BAD_REQUEST`

  (상태 코드는 현재 `GlobalExceptionHandler`에 매핑되어 있는 값을 그대로 옮긴 것이며, 이 TASK가 새로 정하는 것이 아니다.)
- `GlobalExceptionHandler`를 `ResponseEntityExceptionHandler` 상속으로 변경하고, 기존 11개 `@ExceptionHandler` 메서드를 다음 4개로 대체:
  - `@ExceptionHandler(UmtleCustomException::class)` — `ProblemDetail.forStatusAndDetail(exception.status, exception.message ?: "Unexpected error")`
  - `@ExceptionHandler(AccessDeniedException::class)` — `throw exception`(재던지기)
  - `@ExceptionHandler(AuthenticationException::class)` — `throw exception`(재던지기)
  - `@ExceptionHandler(Exception::class)` — ERROR 레벨 로깅(스택트레이스 포함) 후 `ProblemDetail.forStatusAndDetail(INTERNAL_SERVER_ERROR, "Internal server error")` (exception 메시지 미노출)
- `config` 패키지에 `ProblemDetailAuthenticationEntryPoint`(`AuthenticationEntryPoint`, 401 JSON), `ProblemDetailAccessDeniedHandler`(`AccessDeniedHandler`, 403 JSON) 신설.
- `SecurityConfig.securityFilterChain`의 `exceptionHandling` 블록에서 `authenticationEntryPoint`를 `ProblemDetailAuthenticationEntryPoint`로 교체하고 `accessDeniedHandler`를 `ProblemDetailAccessDeniedHandler`로 지정.
- 위 변경에 대한 통합 테스트 추가(Test Scenarios 참고).
- 문서 갱신: `ARCHITECTURE.md` §8 Deferred Decision #2에 해소 각주 추가, `DECISIONS.md` 요약 표에 `ADR-012` 행 추가, `ADR-010` Decision 8 아래에 amend 각주 추가.

## Out of Scope

- 검증 실패(400) 응답의 필드별 상세 포맷 변경(예: `errors: [{field, message}]` 커스텀 구조) — 기존 Spring 기본 `ProblemDetail` 동작을 그대로 유지, 회귀 여부만 테스트로 확인.
- 기존 도메인 예외의 상태 코드 의미 재검토(예: `DuplicateAttendanceException`을 `BAD_REQUEST`에서 `CONFLICT`로 바꿀지 여부) — 별도 논의 필요, 이 TASK는 기존 값을 그대로 이관만 한다.
- `RequestLoggingFilter`/`traceId`/`X-Trace-Id` 메커니즘 변경 — `ADR-010` 범위, 이 TASK는 그대로 재사용만 한다.
- 500 응답 본문에 `traceId`를 포함하는 것 — `X-Trace-Id` 응답 헤더로 이미 상관관계 추적이 가능하므로 본문에 중복 포함하지 않는다.
- `AuthService`/`UserService`의 `BadCredentialsException`/`AccessDeniedException`을 던지는 코드 자체를 수정하는 것 — 이 TASK는 그 예외들이 최종적으로 어떻게 응답되는지(핸들러 계층)만 바꾼다.

## Functional Scenarios

1. 존재하지 않는 리소스(예: 없는 학생 ID)를 조회하면 이전과 동일하게 404 `ProblemDetail`이 반환된다.
2. 이미 존재하는 `loginId`로 회원가입을 시도하면 이전과 동일하게 409 `ProblemDetail`이 반환된다(다른 9개 도메인 예외도 각자 기존 상태 코드로 동일하게 응답).
3. 애플리케이션 코드가 예상하지 못한 예외(예: null 처리 누락으로 인한 `NullPointerException`)를 던지면, 500 상태 코드에 `ProblemDetail` JSON(고정 메시지, 예외 상세 미노출)이 반환되고 서버 로그에 스택트레이스가 ERROR 레벨로 남는다.
4. 로그인하지 않은 사용자가 인증이 필요한 엔드포인트를 호출하면 401 `ProblemDetail` JSON이 반환된다(현재는 빈 본문).
5. 인증은 되어 있지만 권한이 없는 역할이 보호된 엔드포인트를 호출하면 403 `ProblemDetail` JSON이 반환된다(현재는 Spring Security 기본 응답).
6. `UserService` 내부에서 소유권 불일치 등으로 직접 `AccessDeniedException`을 던지는 흐름(예: 다른 보호자의 자녀 정보 접근 시도)도 5번과 동일하게 403 `ProblemDetail` JSON으로 응답된다 — 500으로 새지 않는다.
7. `@Valid` 검증에 실패하는 요청(예: 빈 `loginId`로 회원가입)은 이전과 동일하게 400 `ProblemDetail`로 응답된다(회귀 없음).

## Business Rules

- 500 응답 본문은 예외 종류나 메시지와 무관하게 항상 고정 문구("Internal server error")만 노출한다 — 내부 구현 정보(클래스명, SQL, 스택트레이스 등)를 클라이언트에 반환하지 않는다.
- `UmtleCustomException`을 상속하는 예외의 상태 코드는 예외 자신이 결정하며, `GlobalExceptionHandler`는 그 값을 그대로 사용한다(핸들러가 상태 코드를 재해석하지 않는다).
- Spring Security 예외(`AccessDeniedException`, `AuthenticationException`)는 MVC 계층(`GlobalExceptionHandler`)에서 최종 응답을 만들지 않고 항상 Security 계층(`ProblemDetailAuthenticationEntryPoint`/`ProblemDetailAccessDeniedHandler`)까지 전파되어 응답이 결정된다.

## API Changes

응답 바디 포맷이 바뀌는 엔드포인트/상황(상태 코드는 기존과 동일, 바디만 `ProblemDetail` JSON으로 통일):

| 상황 | 기존 응답 | 변경 후 응답 |
|---|---|---|
| 매핑되지 않은 예외 발생 | Spring Boot 기본 에러 포맷, 500 | `ProblemDetail` JSON, 500 (본문: 고정 메시지) |
| 인증 실패 | 빈 본문, 401 | `ProblemDetail` JSON, 401 |
| 인가 실패(역할 불일치, 또는 서비스 내부 `AccessDeniedException`) | Spring Security 기본 응답, 403 | `ProblemDetail` JSON, 403 |

기존 도메인 예외 11종의 응답(상태 코드, 바디 필드)과 검증 실패(400) 응답은 변경되지 않는다.

## Domain Impact

Domain 계층 자체의 비즈니스 규칙 변경은 없다. 각 도메인의 예외 클래스가 상속하는 상위 타입만 바뀐다(`RuntimeException` → `UmtleCustomException`). 영향받는 파일:

- 신규: `common/domain/UmtleCustomException.kt`
- 수정(상속 대상만 변경, 생성자/메시지 로직 동일): `common/domain/AggregateNotFoundException.kt`, `user/application/DuplicateLoginIdException.kt`, `user/application/DuplicateStudentClaimException.kt`, `user/application/InvalidSignupRequestException.kt`, `user/application/InvalidUserApprovalException.kt`, `lesson/domain/InvalidLessonStateTransitionException.kt`, `classroom/application/InvalidTeacherAssignmentException.kt`, `attendance/application/DuplicateAttendanceException.kt`, `attendance/application/UnassignedAttendanceStudentException.kt`, `homework/application/UnassignedHomeworkStudentException.kt`, `homework/application/InvalidHomeworkUpdateException.kt`, `homework/presentation/InvalidHomeworkRequestException.kt`
- 수정: `common/presentation/GlobalExceptionHandler.kt`
- 신규: `config/ProblemDetailAuthenticationEntryPoint.kt`, `config/ProblemDetailAccessDeniedHandler.kt`
- 수정: `config/SecurityConfig.kt` (`exceptionHandling` 블록만)

## Database Impact

없음.

## Exception and Error Handling

- **핵심 주의사항**: `AccessDeniedException`/`AuthenticationException`을 처리하는 `ExceptionHandlerExceptionResolver`(MVC 디스패처 내부)는 `ExceptionTranslationFilter`(서블릿 필터, MVC보다 바깥쪽)보다 먼저 실행된다. `GlobalExceptionHandler`에 `Exception::class` catch-all만 두고 이 두 예외 타입에 대한 재던지기 핸들러를 빠뜨리면, catch-all이 "가장 근접한 일치"로 이 예외들을 먼저 가로채 500으로 응답해버려 `ProblemDetailAuthenticationEntryPoint`/`ProblemDetailAccessDeniedHandler`가 아예 호출되지 않는다. 구현 시 반드시 `AccessDeniedException`/`AuthenticationException` 재던지기 핸들러를 `Exception::class` catch-all과 함께 추가해야 한다. Test Scenarios의 6번이 이 회귀를 잡기 위한 테스트다.
- `ResponseEntityExceptionHandler` 상속으로 `MethodArgumentNotValidException` 등 내장 MVC 예외 처리는 그대로 유지된다 — `GlobalExceptionHandler`가 이 메서드들을 오버라이드하지 않는 한 기존 동작이 보존된다.
- 500 catch-all은 예외를 삼키지 않고 반드시 로깅 후 `ProblemDetail`을 반환한다(재던지지 않음 — 이 예외는 이미 여기서 최종 처리되어야 하며, `RequestLoggingFilter`의 catch 블록(`ADR-010`)까지 전파시킬 필요는 없다. `GlobalExceptionHandler`가 잡아 정상적으로 500 응답을 만들었으므로 `RequestLoggingFilter`는 접근 로그의 status 필드로 이미 500 발생을 알 수 있다).

## Test Scenarios

- 도메인 예외(예: `DuplicateLoginIdException`) 발생 시 상태 코드/응답 바디가 이 TASK 이전과 동일하다(회귀 테스트, 11개 중 최소 2~3개 대표 케이스).
- 컨트롤러/서비스에서 매핑되지 않은 예외(예: 테스트 전용으로 `RuntimeException`을 강제로 던지는 경로)를 발생시키면 500 + `ProblemDetail` JSON(고정 메시지, exception 상세 미노출)이 반환되고, 캡처된 로그에 ERROR 레벨 스택트레이스가 남는다.
- `@Valid` 검증 실패 요청(예: 빈 `loginId`로 회원가입)이 여전히 400 `ProblemDetail`로 응답된다(회귀 없음).
- 인증 없이 보호된 엔드포인트 호출 시 401 + `ProblemDetail` JSON.
- 인증되었지만 역할이 맞지 않는 엔드포인트 호출 시 403 + `ProblemDetail` JSON.
- `UserService`에서 직접 `AccessDeniedException`을 던지는 흐름(예: 다른 사용자의 정보에 대한 승인/거절 시도, 또는 소유하지 않은 리소스 접근)에서도 403 + `ProblemDetail` JSON(500이 아님을 명시적으로 assert) — Exception and Error Handling의 핵심 주의사항에 대한 회귀 테스트.

## Acceptance Criteria

- 기존 도메인 예외 11종의 응답(상태 코드, 바디)이 이 TASK 이전과 동일하다.
- 매핑되지 않은 예외가 500 `ProblemDetail`(고정 메시지)로 응답되고, 예외 상세(메시지/클래스명/스택트레이스)가 응답 바디에 노출되지 않는다.
- 검증 실패(400) 응답이 이 TASK 이전과 동일하게 동작한다.
- 인증 실패(401), 인가 실패(403) 모두 `ProblemDetail` JSON으로 응답되며, 상태 코드는 기존과 동일하다.
- `UserService`가 직접 던지는 `AccessDeniedException`이 500이 아닌 403으로 정확히 응답된다.
- `GlobalExceptionHandler`의 핸들러 메서드 수가 11개에서 4개로 줄어든다.

## Definition of Done

- `docs/DEVELOPMENT.md` § Definition of Done 기준을 만족한다.
- 이 TASK는 스키마 변경이 없으므로 Flyway 마이그레이션이 필요 없다.
- 위 Test Scenarios가 모두 통과하고, `./gradlew test`가 통과한다.
- `docs/ARCHITECTURE.md` §8, `docs/DECISIONS.md`, `docs/adr/ADR-010-request-tracing-and-logging.md`가 함께 갱신된다.
- Implementation Agent의 self-check 이후, 별도의 독립적인 Review Agent 검토를 거친다(`AGENTS.md`, `CLAUDE.md` 원칙).

## Open Questions

- 기존 도메인 예외의 상태 코드 의미(특히 `DuplicateAttendanceException` 등 `BAD_REQUEST`로 매핑된 "중복" 계열 예외가 `CONFLICT`가 더 적합하지 않은지)는 이번 TASK 범위 밖이다 — 필요시 별도 TASK/ADR로 논의한다.
- `AggregateNotFoundException`이 `UmtleCustomException`을 상속하면서 `abstract class` 2단 상속 구조(`UmtleCustomException` → `AggregateNotFoundException` → `XxxNotFoundException`)가 되는데, 이 정도 깊이가 과설계인지는 구현 리뷰에서 재확인이 필요하다.

## Related ADRs

- `docs/adr/ADR-012-global-exception-handling.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-010-request-tracing-and-logging.md`
