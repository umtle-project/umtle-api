# TASK-009: 요청 추적(traceId)과 접근/쿼리 로깅 도입

## Status

In Review

## Purpose

모든 HTTP 요청에 traceId를 부여해 로그에서 요청 단위로 추적할 수 있게 하고, 접근 로그와 SQL 실행 로그를 기본으로 켜서 "요청이 들어와도 아무것도 로그에 안 보이는" 문제를 해소한다.

## Background

코드 조사 결과 이 프로젝트에는 로깅 관련 코드가 전혀 없었다 — `logback-spring.xml` 없음, 요청 로깅 필터/인터셉터 없음, MDC 사용 없음, `application.yml`에 `logging.*` 설정 없음, `GlobalExceptionHandler`에 로깅 없음. 이 공백과 그 위에서 확정한 결정은 `ADR-010`에서 다뤘다. 이 TASK는 그 결정을 구현한다.

## Related Documents and Requirement IDs

- `docs/adr/ADR-010-request-tracing-and-logging.md` — 이 TASK가 구현하는 결정
- `docs/adr/ADR-003-common-not-found-error-handling.md` — 이 TASK가 건드리지 않는 `GlobalExceptionHandler`의 기존 구조
- `docs/ARCHITECTURE.md` §7 기술 스택, §8 Deferred Decisions(로깅/관측성 항목 없음 — 신규 영역)

## Users and Permissions

해당 없음 — 사용자 역할과 무관한 크로스커팅 인프라 변경이며, 기존 API의 인가 규칙을 바꾸지 않는다.

## Preconditions

- `ADR-010`이 `Accepted` 상태로 전환되어 있어야 한다.

## Scope

- `traceId` 발급과 접근 로그를 함께 수행하는 서블릿 필터 `RequestLoggingFilter` 신설.
- 이 필터를 Spring Security의 `FilterChainProxy`보다 앞단에서 실행되도록 등록하는 설정(`FilterRegistrationBean`) 신설.
- `application.yml`에 SQL 실행 로그(`org.hibernate.SQL=debug`), SQL 포맷팅(`hibernate.format_sql=true`), 콘솔 로그 패턴(`logging.pattern.level`에 `%X{traceId}` 포함) 설정 추가.
- `application-test.yml`에 `org.hibernate.SQL=warn` 오버라이드 추가(테스트 로그 노이즈 감소).
- 위 변경에 대한 통합 테스트 추가.

## Out of Scope

- JSON 구조화 로그(`logstash-logback-encoder`), 로그 수집기(ELK/CloudWatch/Loki) 연동, 분산 트레이싱(Micrometer Tracing) — `ADR-010` Context 참고, 배포 타겟 미정.
- `application-prod.yml`에 대한 로그 레벨 결정 — placeholder 상태 유지.
- SQL 파라미터 바인딩 값 로깅 — `ADR-010` Decision 4.
- MDC에 인증된 사용자 식별자(userId/role) 추가 — `ADR-010` Decision 9.
- `GlobalExceptionHandler`에 대한 catch-all 예외 핸들러 추가 — `ADR-010` Decision 8, Considered Alternatives 4에서 명시적으로 기각.
- 도메인 예외 핸들러 11개 각각에 개별 로깅 추가 — `ADR-010` Considered Alternatives 5에서 기각.
- 요청/응답 바디 로깅, 향후 코드가 지켜야 할 마스킹 구현 — 지금은 그런 로깅 코드 자체가 없으므로 구현 대상이 아니다(`ADR-010` Decision 5의 원칙만 문서로 명문화하고, 실제 마스킹 로직은 그런 로깅이 실제로 추가될 때 별도로 설계한다).

## Functional Scenarios

1. 인증 없이 호출 가능한 엔드포인트(`GET /actuator/health` 등)를 호출하면, 응답 헤더에 `X-Trace-Id`가 존재하고, 서버 로그에 `{method} {path} -> {status} ({durationMs} ms)` 형식의 접근 로그 한 줄이 남으며 그 줄에 같은 traceId가 포함된다.
2. 잘못된 자격증명으로 로그인을 시도해 401을 받는 경우에도(=Spring Security가 요청을 거부하는 경우) 접근 로그와 traceId가 남는다 — 이 필터가 Security 체인보다 앞단에서 실행됨을 증명하는 핵심 시나리오.
3. 서로 다른 두 요청이 (순차적이든 동시적이든) 서로 다른 traceId를 갖는다 — MDC가 요청 스레드마다 격리됨을 확인.
4. 존재하지 않는 리소스를 조회해 `AggregateNotFoundException`이 404로 응답되는 기존 흐름에서, 접근 로그의 status가 404로 정확히 기록된다 — `GlobalExceptionHandler`를 건드리지 않고도 접근 로그만으로 4xx 발생을 알 수 있음을 확인.
5. JPA를 거치는 요청(예: 학생 조회)을 로컬 프로파일에서 호출하면 콘솔에 실행된 SQL 문이 로그로 남는다.

## Business Rules (로깅 규칙)

- 접근 로그는 method, 요청 경로(쿼리스트링 제외), 응답 상태 코드, 소요시간(ms)만 남긴다. 요청/응답 헤더나 바디를 남기지 않는다.
- SQL 로그는 실행된 쿼리문만 남기고, 파라미터 바인딩 값은 남기지 않는다.
- `Authorization`/`Cookie` 헤더, 비밀번호·토큰 값, 요청/응답 바디 전체는 어떤 로그에도 남기지 않는다(`ADR-010` Decision 5) — 이번 TASK가 추가하는 로그는 이미 이 규칙을 지키도록 설계되어 있으므로 별도 마스킹 로직 구현은 필요 없다.
- traceId는 클라이언트가 보낸 값을 신뢰하지 않고 서버가 항상 새로 발급한다.

## API Changes

모든 엔드포인트의 응답에 `X-Trace-Id` 헤더가 새로 추가된다. 기존 요청/응답 바디, 상태 코드, 기존 헤더는 변경하지 않는다.

## Domain Impact

Domain 계층 변경 없음(크로스커팅 인프라 변경). 아래 파일이 대상이다.

- 신규 `config/RequestLoggingFilter.kt` (`OncePerRequestFilter` 상속, `CsrfCookieFilter.kt`와 동일하게 `@Component` 없이 plain class로 작성)
  - `doFilterInternal`: traceId 생성(`UUID.randomUUID().toString()`) → `MDC.put("traceId", traceId)` → `response.setHeader("X-Trace-Id", traceId)` → 시작 시각 기록 → `try { chain.doFilter(request, response) } catch (throwable: Throwable) { ERROR 레벨로 로깅 후 재던짐 } finally { INFO 레벨 접근 로그 기록 → MDC.remove("traceId") }`.
  - 접근 로그와 예외 로그는 이 클래스의 SLF4J 로거(`LoggerFactory.getLogger(RequestLoggingFilter::class.java)`)를 그대로 사용한다. 별도 로거 이름 분리는 하지 않는다.
- 신규 `config/LoggingConfig.kt`
  - `@Bean fun requestLoggingFilter(): FilterRegistrationBean<RequestLoggingFilter>` — `filter = RequestLoggingFilter()`, `order = Ordered.HIGHEST_PRECEDENCE`, URL 패턴은 전체(`/*`)로 등록.
  - `SecurityConfig.kt`는 수정하지 않는다 — 이 필터는 `SecurityFilterChain` 밖, 서블릿 컨테이너 필터 체인에 별도로 등록된다(`ADR-010` Decision 2).
- `src/main/resources/application.yml`
  ```yaml
  logging:
    level:
      org.hibernate.SQL: debug
    pattern:
      level: '%5p [%X{traceId:-}]'
  ```
- `src/main/resources/application-test.yml`
  ```yaml
  logging:
    level:
      org.hibernate.SQL: warn
  ```

## Exception and Error Handling

- `GlobalExceptionHandler`는 수정하지 않는다(`ADR-010` Decision 8, Out of Scope 참고).
- `RequestLoggingFilter`의 catch 블록은 어떤 `HandlerExceptionResolver`도 처리하지 못해 필터 체인 밖으로 전파되는 예외만 만난다 — 이미 `GlobalExceptionHandler`가 매핑한 도메인 예외나 Spring이 기본 처리하는 검증 예외(`MethodArgumentNotValidException` 등)는 이 catch에 도달하지 않는다(정상 흐름으로 응답이 이미 만들어진 뒤 `chain.doFilter()`가 정상 반환되기 때문).
- 이 catch 블록은 예외를 삼키지 않고 로깅 후 반드시 다시 던진다 — 그래야 기존 응답 처리 흐름(컨테이너의 기본 에러 처리)이 그대로 유지된다.

## Test Scenarios

- `GET /actuator/health` 호출 시 응답에 `X-Trace-Id` 헤더가 존재한다.
- 잘못된 로그인 시도(401)에도 `X-Trace-Id` 헤더와 접근 로그가 남는다 — Security 필터보다 먼저 실행됨을 확인하는 핵심 테스트. (Logback `ListAppender`를 테스트에 부착해 로그 이벤트를 캡처하는 방식을 권장.)
- 연속된 두 요청의 `X-Trace-Id` 값이 서로 다르다.
- 존재하지 않는 리소스 조회(404) 시 접근 로그에 status `404`가 기록된다.
- 로컬/기본 프로파일에서 JPA 조회 요청 시 콘솔(또는 캡처된 로그)에 `org.hibernate.SQL` 로거의 SQL 로그가 남는다.
- `test` 프로파일로 테스트를 실행했을 때 `org.hibernate.SQL` 로그가 (기본 테스트 실행 로그 대비) 억제되어 있는지 설정값 기준으로 확인(레벨이 `warn`인지).

## Acceptance Criteria

- 모든 요청(인증 성공/실패 무관)에 대해 서버 로그에 `{method} {path} -> {status} ({durationMs} ms)` 형식의 접근 로그가 남고, 응답에 `X-Trace-Id` 헤더가 포함된다.
- 서버 콘솔 로그의 모든 줄에 해당 요청의 traceId가 노출된다(`logging.pattern.level` 적용 확인).
- 로컬/기본 프로파일에서 SQL 실행문이 로그로 보이고, 파라미터 바인딩 값은 보이지 않는다.
- `test` 프로파일에서는 SQL 로그가 `warn`으로 억제되어 있다.
- 접근 로그, SQL 로그 어디에도 요청/응답 바디, `Authorization`/`Cookie` 헤더, 비밀번호/토큰 값이 노출되지 않는다.
- 기존 `GlobalExceptionHandler`의 11개 도메인 예외 핸들러 동작(응답 바디/상태 코드)이 이 TASK로 인해 전혀 변경되지 않는다(회귀 없음).

## Definition of Done

- `docs/DEVELOPMENT.md` § Definition of Done 기준을 만족한다.
- 이 TASK는 스키마 변경이 없으므로 Flyway 마이그레이션이 필요 없다.
- 위 Test Scenarios가 모두 통과하고, `./gradlew test`(또는 프로젝트 표준 검증 커맨드)가 통과한다.
- Implementation Agent의 self-check 이후, 별도의 독립적인 Review Agent 검토를 거친다(`AGENTS.md`, `CLAUDE.md` 원칙).

## Open Questions

- 인증된 사용자 식별자를 접근 로그/MDC에 포함할지는 이번 범위에서 명시적으로 제외했다(`ADR-010` Decision 9) — 필요해지면 별도 TASK로 확장한다.
- `RequestLoggingFilter`의 catch 블록이 잡는, 어떤 리졸버도 처리하지 못하는 예외가 실제로 얼마나 발생하는지는 알 수 없다(`ADR-010` Risks) — 운영 중 실제로 관찰되면 재검토한다.

## Related ADRs

- `docs/adr/ADR-010-request-tracing-and-logging.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
