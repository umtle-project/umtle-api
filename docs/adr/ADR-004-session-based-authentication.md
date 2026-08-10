# ADR-004: 세션 기반 인증과 Presentation 계층 인가를 채택한다

## Status

Accepted

`TASK-003`에서 세션 기반 인증, CSRF 활성화, Presentation 계층 인가를 실제 구현에 적용했다.

## Date

2026-08-10

## Context

- `ARCHITECTURE.md` 8장 Deferred Decision #3("인증/인가 구현 방식 — 세션 기반/토큰 기반 여부, 인가 처리 위치")는 "`USER_ROLES.md` 확정 후 결정"이라는 조건을 걸고 보류되어 왔다. `TASK-001`로 `USER_ROLES.md`가 이미 작성·머지되어(관리자/선생님/학생/학부모 4개 역할과 도메인별 권한 매트릭스 정형화) 이 조건이 충족되었다.
- `TASK-002`(학생 관리 도메인)는 "Spring Security 기반 인증/인가 강제"를 명시적으로 Out of Scope로 남겼다. 그 결과 현재 `SecurityConfig.kt`는 모든 요청을 인증 없이 허용하는 임시 상태다(`authorizeHttpRequests { authorize(anyRequest, permitAll) }`, CSRF 비활성화) — 실제 보안 공백이며, `SecurityConfig.kt` 자체에 "Deferred Decision #3 미확정 상태라 전체 허용한다"는 주석이 남아 있다.
- `spring-boot-starter-security`, `spring-boot-starter-security-test`는 부트스트랩(`TASK-000`) 시점에 이미 Gradle 의존성으로 추가되어 있으나, 지금까지 실제로 구성된 적이 없다.
- `PRD.md` 1장은 움틀을 "웹 기반 서비스"로만 정의한다 — 별도 모바일 네이티브 앱이나 제3자 API 소비자는 어느 문서에도 등장하지 않는다.
- `DOMAIN_MODEL.md` 3.1은 User(사용자) 도메인의 책임을 "시스템에 접근하는 주체를 구분하고 역할을 표현"으로 정의하지만, 인증/인가의 구체적 구현 방식은 명시적으로 이 문서의 범위 밖으로 두고 있다.
- User 도메인 자체가 아직 코드에 존재하지 않는다 — 계정, 자격 증명, 역할 저장이 전무하다. 이 ADR의 결정은 뒤따르는 `TASK-003`이 User 도메인과 인증/인가를 함께 구현할 수 있게 하는 전제 조건이다.

## Decision

- **인증 방식**: 세션 기반 인증을 채택한다. 토큰(JWT 등) 기반 인증은 채택하지 않는다.
  - 로그인 성공 시 서버가 `HttpSession`을 생성하고 `SecurityContext`를 세션에 저장한다(`HttpSessionSecurityContextRepository`, Spring Security 기본 동작).
  - 클라이언트는 세션 쿠키로 인증 상태를 유지한다. 별도의 access/refresh 토큰 발급·저장·갱신 로직을 두지 않는다.
- **인가 처리 위치**: Presentation 계층에서 처리한다.
  - 경로/HTTP 메서드 단위의 접근 제어는 `SecurityConfig`의 `authorizeHttpRequests`로 선언한다.
  - 더 세밀한 규칙이 필요한 경우에 한해 컨트롤러 메서드에 `@PreAuthorize`를 사용한다.
  - Application Service(예: `StudentService`)와 Domain 계층에는 Spring Security 관련 어노테이션이나 권한 체크 코드를 두지 않는다 — Domain이 JPA를 모르는 것과 같은 원칙으로, Domain/Application은 인증 프레임워크를 모른다.
- CSRF는 비활성화 상태를 유지하지 않는다. 세션 쿠키 기반 인증에서는 CSRF 공격이 유효하므로, `CookieCsrfTokenRepository`를 사용해 활성화한다.

## Decision Drivers

- `PRD.md`가 명시하는 클라이언트는 웹 하나뿐이며, 두 번째 클라이언트 유형(모바일 네이티브 앱, 제3자 API 등)이 어느 문서에도 계획되어 있지 않다 — 토큰 기반 인증의 핵심 이점(무상태성, 여러 클라이언트/서버 간 공유)을 지금 활용할 근거가 없다.
- `ADR-001`(단일 모놀리스로 시작한다)이 이미 "팀 규모와 배포 빈도상 근거가 없는 복잡성은 들이지 않는다"는 판단을 내렸다 — 토큰 기반 인증은 서명 키 관리, 토큰 폐기(revocation) 전략, 재발급(refresh) 흐름 등 지금 시점에 근거 없는 복잡성을 추가한다.
- `spring-boot-starter-security`가 이미 의존성으로 존재하고, 세션 기반 인증은 Spring Security의 기본 동작을 그대로 사용해 최소 설정으로 구성 가능하다.
- Domain/Application 계층이 프레임워크에 의존하지 않는다는 기존 원칙(`ARCHITECTURE.md` 6장, "JPA 연관관계 사용 금지"의 근거와 같은 결)과 일관되게, 인가도 Presentation 계층에 위치시키는 것이 계층 간 책임 분리를 유지한다.
- Spring Security의 `authorizeHttpRequests`/`@PreAuthorize`는 선언적이라, 도메인이 늘어나도(반/수업, 출결 등) 권한 규칙을 한눈에 감사(audit)하기 쉽다. Application 계층에 수동 체크를 흩어 두면 도메인이 늘어날수록 누락·불일치 위험이 커진다.

## Considered Alternatives

### 1. 토큰(JWT) 기반 인증

- 설명: 로그인 시 서명된 JWT(access token, 필요시 refresh token)를 발급하고, 클라이언트가 이후 요청마다 `Authorization` 헤더로 전달한다.
- 기각 사유: 현재 문서화된 클라이언트가 웹 하나뿐이라 무상태성·다중 클라이언트 공유라는 토큰 방식의 핵심 이점을 활용할 근거가 없다. 서명 키 관리, 토큰 폐기 전략(로그아웃/탈취 시 즉시 무효화가 세션보다 어려움), 재발급 흐름까지 지금 설계하는 것은 `ADR-001`의 "근거 없는 복잡성 회피" 원칙에 어긋난다. 모바일 앱이나 외부 API 클라이언트가 실제로 계획되는 시점에 재검토한다.

### 2. Application 계층에서 수동 권한 체크

- 설명: `SecurityConfig`는 인증(로그인 여부)만 담당하고, 역할별 접근 제어는 각 Application Service 메서드 시작부에서 `if (!currentUser.hasRole(...)) throw ...` 형태로 수동 체크한다.
- 기각 사유: Spring Security가 이미 제공하는 선언적 인가 기능과 기능이 중복된다. 도메인이 늘어날수록(반/수업, 출결, 숙제 등) 서비스 메서드마다 체크를 반복/누락할 위험이 커지고, 권한 규칙이 여러 파일에 흩어져 한눈에 감사하기 어렵다. 또한 Application 계층이 인증 주체(현재 로그인한 사용자)를 알아야 하므로 Spring Security 컨텍스트에 결합되어, 이 ADR이 목표로 하는 "Application은 인증 프레임워크를 모른다" 원칙과 충돌한다.

### 3. (채택) 세션 기반 인증 + Presentation 계층 인가

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 제안.

## Consequences

### Positive

- 최소 설정으로 현재의 보안 공백(`permitAll`)을 즉시 해소할 수 있다 — 이미 있는 의존성과 Spring Security 기본 기능만 사용한다.
- 인가 규칙이 `SecurityConfig` 한 곳(+ 필요시 `@PreAuthorize`)에 선언적으로 모여 있어 도메인이 늘어나도 감사하기 쉽다.
- Domain/Application 계층이 인증 프레임워크와 분리되어 테스트하기 쉽고, 다른 인증 방식으로 전환하더라도 Domain/Application 코드는 영향받지 않는다.

### Negative

- 세션은 서버 측에 상태를 유지해야 한다(스티키 세션 또는 세션 스토어 공유가 필요) — 여러 인스턴스로 수평 확장할 경우 이 부분을 별도로 다뤄야 한다. 현재는 `ADR-001`(단일 모놀리스, 단일 인스턴스 전제)과 일치해 문제가 되지 않는다.
- 향후 모바일 앱이나 외부 API 클라이언트가 필요해지면 세션 쿠키 방식에서 토큰 방식으로 전환하는 비용이 발생한다.

### Risks

- CSRF를 함께 활성화하지 않으면 세션 쿠키 기반 인증은 CSRF 공격에 노출된다 — 이 ADR의 Decision에 CSRF 활성화를 포함시킨 이유다. 구현 시 반드시 함께 적용해야 한다.

## Validation

모바일 네이티브 앱, 제3자 API 클라이언트 등 두 번째 클라이언트 유형이 실제로 계획되는 시점에 토큰 기반 인증으로의 전환을 재검토한다. 그 전까지는 별도 트리거 없이 재검토하지 않는다.

## Related Documents

- `docs/ARCHITECTURE.md` 8장 Deferred Decision #3(이 ADR로 해소)
- `docs/adr/ADR-001-start-with-monolith.md`
- `docs/USER_ROLES.md`
- `docs/tasks/TASK-003-user-authentication.md`
