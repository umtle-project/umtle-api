# ADR-010: 요청 추적(traceId)과 접근/쿼리 로깅 기본 방침을 도입한다

## Status

Accepted

## Date

2026-08-14

## Context

- 현재 요청이 들어와도 로그에 아무것도 남지 않아 추적이 불가능하다는 문제가 제기되었다. 코드를 확인한 결과 다음이 모두 확인되었다.
  - `logback-spring.xml` 없음 — Spring Boot 기본 콘솔 로깅만 사용.
  - 요청을 로깅하는 필터/인터셉터가 전혀 없음(`CsrfCookieFilter`만 존재하며 로깅 목적이 아니다).
  - MDC를 쓰는 코드가 전혀 없음 — traceId/requestId 개념 자체가 없다.
  - `application.yml`에 `logging.*` 설정이 전혀 없음 — SQL 실행 로그도 켜져 있지 않다.
  - `common/presentation/GlobalExceptionHandler.kt`의 도메인 예외 핸들러 11개 중 어디에도 로깅 코드가 없다.
  - `application-prod.yml`은 아직 placeholder다(배포 타겟 미정, 파일 상단 주석 참고). `docs/ARCHITECTURE.md` 8장 Deferred Decisions에도 로깅/관측성 항목은 없다 — 이 프로젝트에서 완전히 새로운 영역이다.
- 사용자와 범위를 확정한 결과, 이번 결정은 다음으로 한정한다.
  - **포함**: 요청 단위 traceId 발급/MDC 전파, 요청 접근 로그(method/URI/status/소요시간), SQL 실행문 로그(파라미터 바인딩 값 제외), profile별 로그 레벨 정책, 민감정보 마스킹 원칙, 예상치 못한 예외의 로깅 표준화.
  - **제외**: JSON 구조화 로그, 로그 수집기(ELK/CloudWatch/Loki) 연동, 분산 트레이싱(Micrometer Tracing) — 배포 타겟이 아직 없어 지금 확정하면 근거 없이 앞서가는 결정이 된다(`docs/ARCHITECTURE.md` 1장 "근거가 부족한 항목은 임의로 확정하지 않는다"와 동일한 기준).

## Decision

1. **traceId 발급/전파**: 요청마다 서버가 `UUID.randomUUID().toString()`으로 `traceId`를 새로 생성한다. 클라이언트가 보낸 헤더 값은 신뢰하지 않고 항상 새로 발급한다. `MDC`에 키 `traceId`로 저장하고, 응답 헤더 `X-Trace-Id`로 그대로 돌려준다.
2. **필터 위치**: traceId 발급과 접근 로그를 하나의 서블릿 `Filter`(`RequestLoggingFilter`)가 함께 수행하며, Spring Security의 `FilterChainProxy`보다 앞단에서 실행되도록 `FilterRegistrationBean`으로 등록하고 순서를 `Ordered.HIGHEST_PRECEDENCE`로 지정한다. 이렇게 해야 인증/인가 실패로 거부된 요청, CORS preflight 거부 등 Security 체인 내부에서 끝나는 요청도 traceId와 접근 로그를 남길 수 있다.
3. **접근 로그 내용**: HTTP method, 요청 경로(`request.requestURI`), 응답 상태 코드, 소요 시간(ms)만 INFO 레벨로 남긴다. 쿼리스트링, 요청/응답 헤더, 요청/응답 바디는 포함하지 않는다(민감정보 노출 방지, 아래 5번).
4. **SQL 로그**: `logging.level.org.hibernate.SQL=debug`로 실행되는 SQL 문만 로그에 남긴다. 가독성을 위해 `hibernate.format_sql=true`로 SQL을 포맷팅한다. 파라미터 바인딩 값(`org.hibernate.orm.jdbc.bind`)은 켜지 않는다 — 바인딩 값에는 개인정보가 포함될 수 있다.
5. **민감정보 마스킹 원칙**: 이번 구현 범위(접근 로그, SQL 로그) 자체는 위 3, 4번 결정으로 이미 안전하다. 이와 별개로, 앞으로 어떤 코드도 다음을 로그에 남기지 않는다는 원칙을 명문화한다 — 요청/응답 바디 전체, `Authorization`/`Cookie` 헤더, 비밀번호·토큰 필드. 향후 요청/응답 바디를 로깅해야 하는 요구가 생기면 그때 마스킹 방식을 별도로 설계한다(지금은 그런 코드가 없으므로 구현하지 않는다).
6. **콘솔 로그 패턷**: 새 `logback-spring.xml`을 만들지 않는다. 대신 Spring Boot가 제공하는 `logging.pattern.level` 프로퍼티(`application.yml`)로 기본 콘솔 패턴의 레벨 구간에 `%X{traceId:-}`를 주입해, 모든 로그 라인에 traceId가 노출되게 한다.
7. **로그 레벨 정책(profile별)**:

   | Profile | `org.hibernate.SQL` | 비고 |
   |---|---|---|
   | local (기본 활성) | `debug` | 콘솔에서 SQL 실행 흐름 확인 |
   | test | `warn` | 테스트 실행 로그 노이즈 감소 |
   | prod | 미정 | `application-prod.yml`이 아직 placeholder — 배포 타겟이 정해질 때 함께 결정 |

   root 레벨은 Spring Boot 기본값(INFO)을 그대로 둔다.
8. **예상치 못한 예외 로깅**: `GlobalExceptionHandler`에 `@ExceptionHandler(Exception::class)` catch-all을 추가하지 않는다(이유는 Considered Alternatives 참고). 대신 `RequestLoggingFilter`가 `chain.doFilter(...)` 호출을 `try/catch`로 감싸, 어떤 `HandlerExceptionResolver`도 처리하지 못하고 필터 체인 밖으로 전파되는 예외만 ERROR 레벨로 스택트레이스와 함께 1회 로깅한 뒤 그대로 다시 던진다. 이미 `GlobalExceptionHandler`가 매핑해 4xx로 응답하는 도메인 예외들은 이 필터의 catch에 도달하지 않으므로 별도 로깅을 추가하지 않는다 — 접근 로그의 상태 코드로 4xx 발생 여부를 이미 알 수 있다. *(`ADR-012`(Proposed)로 amend됨 — Considered Alternatives #4 하단 각주 참고.)*
9. **MDC 범위**: `traceId`만 MDC에 담는다. 인증된 사용자 식별자(userId/role)는 이번 범위에 포함하지 않는다.

## Decision Drivers

- 사용자가 확정한 스코프(Tier 1 필수 + Tier 2 후속, SQL은 파라미터 미포함, MDC는 traceId만)를 그대로 따른다.
- 배포 타겟 미정 상태에서 로그 수집기/구조화 포맷을 확정하는 것은 근거가 부족하다(`ARCHITECTURE.md` 1장 원칙).
- 이 코드베이스에는 로깅 관련 코드/컨벤션이 전무하므로, 최소 골격만 세우고 과설계를 피한다.
- "요청이 안 보인다"는 원 문제는 인증 실패 등 Security 앞단에서 끝나는 요청일 가능성이 높으므로, 필터가 Security보다 먼저 실행되어야 한다.

## Considered Alternatives

### 1. `SecurityConfig`의 `http.addFilterBefore(...)`로 Security 체인 내부에 필터 추가

- 설명: `CsrfCookieFilter`처럼 `HttpSecurity` 빌더 안에서 필터 순서를 지정.
- 기각 사유: `FilterChainProxy` 자체가 서블릿 컨테이너 필터 체인의 항목 중 하나일 뿐이다. 그 안에 추가해도 `FilterChainProxy` 진입 이전에 끝나는 처리(예: CORS preflight 거부)는 여전히 못 잡는다. 서블릿 컨테이너 레벨의 `FilterRegistrationBean`으로 등록해야 완전히 앞단에 위치한다.

### 2. 클라이언트가 보낸 `X-Request-Id` 등 헤더 값을 traceId로 재사용

- 설명: 클라이언트/프록시가 이미 상관관계 ID를 보냈다면 그것을 그대로 사용.
- 기각 사유: 클라이언트가 임의 값을 주입해 로그를 오염시키거나 다른 요청의 traceId를 위조할 수 있다. 서버가 항상 새로 발급하는 쪽이 안전하고, 지금은 클라이언트(프론트엔드)가 이런 헤더를 보내는 계약도 없다.

### 3. 커스텀 `logback-spring.xml` 도입

- 설명: 최초 범위 산정 시점에는 이 방식을 가정했다.
- 기각 사유(재검토): Spring Boot가 제공하는 `logging.pattern.level` 프로퍼티만으로 기본 콘솔 패턴에 `%X{traceId}`를 주입할 수 있어, 새 XML 파일이나 별도 인코더 없이 동일한 요구를 충족한다. prod 프로파일이 아직 placeholder라 별도 appender 전략(JSON 인코더 등)이 필요 없는 지금 시점에는 프로퍼티 방식이 더 단순하다. JSON 구조화 로그가 실제로 필요해지면(Tier 3) 그때 `logback-spring.xml` 도입을 다시 검토한다.

### 4. `GlobalExceptionHandler`에 `@ExceptionHandler(Exception::class)` catch-all 추가

- 설명: 다른 도메인 예외 핸들러들과 같은 `@RestControllerAdvice`에 모든 예외를 잡는 fallback을 추가해 로깅.
- 기각 사유(확인함): Spring MVC의 `HandlerExceptionResolverComposite`는 `@ControllerAdvice`의 `@ExceptionHandler`를 처리하는 `ExceptionHandlerExceptionResolver`를 가장 먼저 실행한다. 같은 advice에 `Exception::class` 핸들러가 있으면, `MethodArgumentNotValidException` 등 Spring이 기본으로 400 `ProblemDetail`로 변환해주던 예외까지 이 핸들러가 가로채 500으로 바꿔버리는 회귀가 발생한다. 대신 `RequestLoggingFilter`에서 `chain.doFilter()`를 감싸 처리되지 않은 예외만 로깅하는 방식(Decision 8)을 채택했다.

> **Amended by `ADR-012`(Proposed)**: `GlobalExceptionHandler`가 `ResponseEntityExceptionHandler`를 상속하면 내장 MVC 예외(`MethodArgumentNotValidException` 등) 처리는 그대로 보존되어, 이 대안이 우려한 회귀 없이 `Exception::class` catch-all을 안전하게 추가할 수 있다는 방법을 찾았다(단, Spring Security 예외는 별도로 재던지기 처리가 필요 — `ADR-012` Decision Drivers 참고). `ADR-012`가 `Accepted`로 전환되면 Decision 8은 그 결정으로 대체된다. 이 문서 전체의 Status는 나머지 결정(1~7, 9)이 여전히 유효하므로 `Accepted`로 유지한다.

### 5. 도메인 예외 핸들러 11개 각각에 개별 로깅 추가

- 설명: `GlobalExceptionHandler`의 각 `@ExceptionHandler` 메서드 안에 로그 호출 추가.
- 기각 사유: 접근 로그의 상태 코드 필드로 4xx 발생 여부는 이미 추적 가능하다. 11곳에 반복 코드를 추가하는 것은 과설계다.

## Consequences

### Positive

- 모든 요청(인증 실패 포함)에 traceId가 부여되고 접근 로그로 남아, 원래 문제("요청이 안 보임")가 해소된다.
- SQL 실행 여부를 즉시 확인할 수 있다.
- 새 파일/의존성을 최소화했다(`logback-spring.xml` 없음, 신규 3rd-party 로깅 라이브러리 없음).
- `GlobalExceptionHandler`의 기존 400 처리 회귀 위험을 사전에 확인하고 피했다.

### Negative

- `RequestLoggingFilter`가 서블릿 컨테이너 필터 체인의 최상단(`Ordered.HIGHEST_PRECEDENCE`)을 차지하므로, 이후 다른 전역 필터(예: rate limiting)를 추가할 때 순서를 함께 조율해야 한다.
- MDC에 `traceId`만 담기로 했으므로, 접근 로그만으로는 "누가" 요청했는지 알 수 없다. 필요해지면 별도로 범위를 확장해야 한다.

### Risks

- `FilterRegistrationBean`의 order를 잘못 지정하면(값을 누락하거나 Security보다 큰 값을 주면) 의도한 실행 순서가 깨져 인증 실패 요청이 여전히 추적되지 않을 수 있다 — 구현 후 통합 테스트로 검증이 필요하다(`TASK-009` Test Scenarios).
- 필터가 `chain.doFilter()`를 감싸는 catch 블록에서 예외를 재던지기 전에 접근 로그를 남기는데, 이 시점에는 컨테이너가 아직 최종 500 상태 코드를 응답에 반영하기 전일 수 있어 그 아주 드문 경우(어떤 `HandlerExceptionResolver`도 처리하지 못하는 완전히 처리되지 않은 예외)에는 접근 로그의 status 필드가 부정확할 수 있다. Spring Boot의 기본 리졸버가 대부분의 예외를 처리하므로 실무에서 발생 빈도는 낮다고 보고 지금은 감수한다.

## Validation

실제 배포 타겟이 정해지고 로그 수집기 연동이 필요해지는 시점에 Tier 3(JSON 구조화 로그, 분산 트레이싱)를 별도로 재논의한다. 별다른 트리거 없이는 재검토하지 않는다.

## Related Documents

- `docs/ARCHITECTURE.md` — 8장 Deferred Decisions에 로깅/관측성 항목 없음(신규 영역)
- `docs/adr/ADR-003-common-not-found-error-handling.md` — `GlobalExceptionHandler`의 기존 구조, 이 ADR이 건드리지 않는 이유(Decision 8, Considered Alternatives 4)
- `docs/adr/ADR-012-global-exception-handling.md` — Decision 8(catch-all 미도입)을 amend
- `docs/tasks/TASK-009-request-tracing-and-logging.md` — 이 ADR을 구현하는 TASK
