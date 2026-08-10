# TASK-003: User 도메인 및 인증/인가 구현

## Status

Ready

## Purpose

User(사용자/계정) 도메인을 도입하고, `SecurityConfig`의 임시 전체 허용(`permitAll`) 상태를 실제 인증/인가로 교체해 현재의 보안 공백을 해소한다. `ARCHITECTURE.md` Deferred Decision #3을 해소하는 `ADR-004`의 결정을 실제로 적용하는 작업이다.

## Background

`TASK-002`(학생 관리 도메인)는 "Spring Security 기반 인증/인가 강제"를 명시적으로 Out of Scope로 남겼고, 그 결과 `SecurityConfig.kt`는 모든 요청을 인증 없이 허용하는 임시 상태로 남아 있다. `TASK-001`로 `USER_ROLES.md`가 역할·권한 매트릭스를 정형화했고, `ADR-004`가 인증 방식(세션 기반)과 인가 처리 위치(Presentation 계층)를 결정했다. User 도메인은 다른 모든 도메인이 "행위 주체"로 참조하는 중심 개념(`DOMAIN_MODEL.md` 3.1)이지만 아직 코드에 전혀 존재하지 않는다.

## Related Documents and Requirement IDs

- `docs/ARCHITECTURE.md` 8장 Deferred Decision #3 (이 작업으로 해소)
- `docs/USER_ROLES.md` 3.1 학생 관리 (권한 매트릭스 적용 대상)
- `docs/DOMAIN_MODEL.md` 3.1 사용자(User)
- `docs/adr/ADR-002-tsid-as-identifier.md` (ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md` (Not Found 공통 처리, Proposed — 이 작업에서 검증)
- `docs/adr/ADR-004-session-based-authentication.md` (인증/인가 방식 결정)

## Users and Permissions

`USER_ROLES.md` 3.1을 그대로 적용한다 — 학생 관리(Student) 엔드포인트 기준:

| 역할 | 권한 |
|------|------|
| 관리자 | 학생 등록, 조회, 수정, 비활성화 (전체 CRUD) |
| 선생님 | 학생 조회만 (담당 학생 스코핑은 반/수업 도메인 부재로 여전히 불가 — `TASK-002`와 동일한 제약) |
| 학생 | `USER_ROLES.md`에 서술 없음 → **기본 거부**. 임의로 권한을 부여하지 않는다. |
| 학부모 | `USER_ROLES.md`에 서술 없음 → **기본 거부**. |

User 자신의 계정 관리(계정 생성/조회) 권한은 `REQUIREMENTS.md`에 별도 기능 요구사항으로 명시되어 있지 않다 — 아래 "Open Questions" 참고. 이 작업에서는 관리자만 계정을 생성/조회할 수 있는 것으로 최소 범위를 가정한다.

## Preconditions

`TASK-001`(`USER_ROLES.md`), `TASK-002`(Student 도메인), `ADR-004`(Proposed 상태로 존재, 이 작업이 실제 적용 사례가 됨)가 존재해야 한다.

## Scope

### User 도메인

- `user/domain/User.kt` — `Student.kt`와 동일한 패턴(private 생성자 + `register(loginId, passwordHash, roles)` / `reconstitute(...)` companion factory, `require()`로 불변식 검증). ID는 TSID `Long`, `@GeneratedValue` 없이 factory에서 직접 할당(`ADR-002`).
- 필드: `id`, `loginId`(로그인 식별자, unique), `passwordHash`(이미 해시된 값을 받는다 — 원문 비밀번호 해싱은 Application 계층의 책임이며 Domain은 `PasswordEncoder`를 모른다), `roles`(`Set<UserRole>`, 최소 1개 이상), `status`(`ACTIVE`/`INACTIVE`, `StudentStatus`와 동일한 패턴).
- `user/domain/UserRole.kt` — enum `ADMIN`, `TEACHER`, `STUDENT`, `PARENT` (`DOMAIN_MODEL.md` 3.1과 동일한 4개 역할).
- `user/domain/UserStatus.kt` — enum `ACTIVE`, `INACTIVE`.
- `user/domain/UserNotFoundException.kt`, `user/domain/UserRepository.kt` (포트).
- `user/infrastructure/UserJpaEntity.kt` — `BaseEntity` 상속. 역할은 `@ElementCollection` + `@CollectionTable(name = "user_roles")`로 저장한다 — 이는 다른 Aggregate에 대한 참조가 아니라 User가 소유하는 값 컬렉션이므로 `ARCHITECTURE.md` 6장의 "JPA 연관관계 사용 금지"(Aggregate 간 참조 금지) 원칙과 충돌하지 않는다.
- `user/infrastructure/UserJpaRepository.kt`, `UserRepositoryAdapter.kt`.
- `user/application/UserService.kt` — 계정 생성(관리자 전용, `PasswordEncoder`로 원문 비밀번호를 해싱해 `User.register`에 전달), 조회.
- `user/application/AuthService.kt` — 로그인 검증(자격 증명 대조), 필요 시 `AuthenticationManager`로 위임.
- `user/presentation/UserController.kt` (`POST /api/v1/users`, `GET /api/v1/users/{id}` — 관리자 전용), `UserDtos.kt`, `AuthController.kt` (`POST /api/v1/auth/login`, `POST /api/v1/auth/logout`).

### 인증/인가 인프라

- `SecurityConfig.kt` 재작성:
  - `/api/v1/auth/login`, `/actuator/health`만 인증 없이 허용. 그 외 모든 요청은 `authenticated()`.
  - `/api/v1/students/**`에 위 권한 매트릭스 적용: `GET` → `hasAnyRole("ADMIN", "TEACHER")`, 그 외 메서드 → `hasRole("ADMIN")`.
  - `/api/v1/users/**`는 `hasRole("ADMIN")`.
  - CSRF는 `CookieCsrfTokenRepository.withHttpOnlyFalse()`로 활성화(`ADR-004` Risks 참고) — 더 이상 `csrf { disable() }`를 두지 않는다.
  - `BCryptPasswordEncoder` 빈 등록.
  - `UserRepository`를 통해 `User`를 로드하는 `UserDetailsService` 구현체 — 역할을 `ROLE_` 접두사 `GrantedAuthority`로 매핑.
  - 로그인 성공 시 `SecurityContext`를 세션에 저장(`HttpSessionSecurityContextRepository`, Spring Security 기본 동작), 로그아웃 시 세션 무효화.
- 최초 관리자 계정 부트스트랩: `config/BootstrapAdminRunner.kt`(`ApplicationRunner`) — 기동 시 `ADMIN` 역할을 가진 User가 하나도 없으면, 설정 프로퍼티(`umtle.bootstrap-admin.login-id` / `umtle.bootstrap-admin.password`, 환경변수로 주입)로 최초 관리자 계정을 생성한다. Flyway 시드 마이그레이션 방식은 채택하지 않는다 — 비밀번호 해시가 버전관리되는 SQL 파일에 커밋되는 것을 피하기 위함이다. 로컬 개발용 기본값은 `application-local.yml`에만 둔다.

### ADR-003 검증 및 마이그레이션

- `UserNotFoundException`을 `common.domain.AggregateNotFoundException`(신규, ADR-003이 제안한 추상 클래스) 상속으로 구현하고, `common.presentation`에 단일 전역 `@RestControllerAdvice`를 두어 404 `ProblemDetail`로 변환한다.
- User 도메인에 이 패턴을 적용해본 결과가 잘 맞으면(2번째 도메인에서의 검증, `ADR-003` Validation 조건 충족):
  - `ADR-003` 상태를 `Accepted`로 전환하고, `docs/DECISIONS.md`에 한 줄 추가한다.
  - 기존 `student/domain/StudentNotFoundException.kt`, `student/presentation/StudentExceptionHandler.kt`를 제거하고 `Student`도 같은 공통 패턴(`AggregateNotFoundException` 상속)으로 마이그레이션한다.
- 로그인 실패(자격 증명 불일치)는 "not found"가 아니라 인증 실패이므로 `AggregateNotFoundException`으로 다루지 않고 Spring Security의 401 처리로 별도 취급한다.

### 데이터베이스

- `V2__create_users_table.sql`:

```sql
CREATE TABLE users (
    id BIGINT NOT NULL,
    login_id VARCHAR(50) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_users_login_id UNIQUE (login_id)
);

CREATE TABLE user_roles (
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    PRIMARY KEY (user_id, role),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id)
);
```

## Out of Scope

- 셀프 회원가입(가입 신청/승인) 정책 — 계정은 관리자가 생성한다고 가정한다(아래 Open Questions 참고).
- 학부모-학생 연결 방식, "담당 학생"/"담당 반" 스코핑 — `USER_ROLES.md` 4장, `DOMAIN_MODEL.md` 6장에 이미 미정으로 명시.
- 역할 간 위계(상위 역할이 하위 역할 권한을 포함하는지) — `USER_ROLES.md` 4장 미정 항목.
- Student 외 도메인(반/수업, 출결 등)의 인가 규칙 — 해당 도메인이 아직 구현되지 않음.
- 토큰(JWT) 기반 인증 — `ADR-004`에서 명시적으로 보류.
- 비밀번호 재설정, 계정 잠금, 로그인 시도 제한 등 부가 보안 기능 — 실제 요구가 생기면 별도 작업으로 다룬다.

## Functional Scenarios

- 관리자가 `loginId`/비밀번호로 계정을 생성하면, 지정한 역할(들)을 가진 User가 `ACTIVE` 상태로 생성된다.
- 등록된 사용자가 올바른 `loginId`/비밀번호로 로그인하면 세션이 발급되고, 이후 요청은 해당 세션 쿠키로 인증된다.
- 잘못된 자격 증명으로 로그인하면 401을 반환하고 세션이 발급되지 않는다.
- 로그아웃하면 세션이 무효화되어, 이후 같은 세션 쿠키로 보호된 엔드포인트에 접근하면 401을 반환한다.
- 인증되지 않은 상태로 `/api/v1/students/**`에 접근하면 401을 반환한다.
- `TEACHER` 역할로 로그인한 사용자가 학생 조회(`GET`)는 성공하지만, 등록/수정/비활성화를 시도하면 403을 반환한다.
- `ADMIN` 역할로 로그인한 사용자는 학생 관련 API 전체와 계정 생성/조회 API에 접근할 수 있다.
- 애플리케이션을 최초 기동했을 때 `ADMIN` 계정이 하나도 없으면 부트스트랩 관리자 계정이 자동 생성되고, 이미 있으면 다시 생성되지 않는다.

## Business Rules

- `loginId`는 공백일 수 없고 중복될 수 없다(DB unique 제약 + 애플리케이션 검증).
- 비밀번호는 평문으로 저장하지 않는다 — 항상 `BCryptPasswordEncoder`로 해싱한 값만 저장한다.
- User는 하나 이상의 역할을 가져야 한다(`DOMAIN_MODEL.md` 3.1 "하나 이상의 역할").

## API Changes

- `POST /api/v1/auth/login` — 200, body `{ "loginId": string, "password": string }` → 세션 쿠키 발급 / 401(자격 증명 불일치)
- `POST /api/v1/auth/logout` — 200, 세션 무효화
- `POST /api/v1/users` — 201(관리자 전용), body `{ "loginId": string, "password": string, "roles": string[] }` → `UserResponse`
- `GET /api/v1/users/{id}` — 200(관리자 전용) / 404
- `UserResponse`: `{ "id": number, "loginId": string, "roles": string[], "status": "ACTIVE" | "INACTIVE" }` (비밀번호 해시는 응답에 포함하지 않는다)
- 기존 `/api/v1/students/**` 엔드포인트는 그대로 유지하되, 위 Users and Permissions 표에 따른 인증/인가가 강제된다.

## Domain Impact

`DOMAIN_MODEL.md` 3.1 사용자(User) Aggregate를 신규로 도입한다. 기존 `Student` Aggregate는 도메인 로직 자체는 변경하지 않으며, 예외 처리만 ADR-003 공통 패턴으로 마이그레이션한다(위 Scope 참고). User는 다른 Aggregate를 참조하지 않는다.

## Database Impact

`V2__create_users_table.sql` — `users`, `user_roles` 테이블 신규 생성(위 Scope 참고).

## Exception and Error Handling

- 인증 실패(잘못된 자격 증명, 미인증 상태로 보호된 리소스 접근) → 401.
- 인가 실패(인증은 됐으나 역할 부족) → 403.
- 존재하지 않는 User 조회 → 404 (`UserNotFoundException`, ADR-003 공통 패턴).
- 계정 생성 시 `loginId` 중복 → 400 또는 409(구현 시 결정 — 이 문서는 상태 코드를 하드코딩하지 않고 "클라이언트 오류로 처리"만 요구한다. 리뷰 시 최종 코드 확인).

## Test Scenarios

- `UserTest`(도메인 단위): 등록 시 검증(빈 `loginId` 거부, 역할 최소 1개 요구), `reconstitute`.
- 로그인 성공/실패 통합 테스트(MockMvc): 올바른 자격 증명 → 세션 쿠키 발급, 잘못된 자격 증명 → 401.
- 로그아웃 후 동일 세션으로 보호된 엔드포인트 접근 시 401.
- 미인증 상태로 `/api/v1/students` 접근 → 401.
- `TEACHER`로 `POST /api/v1/students` 접근 → 403, `GET /api/v1/students` → 200.
- `ADMIN`으로 학생 API 전체 및 `/api/v1/users` 접근 → 200/201.
- `BootstrapAdminRunner`: 최초 기동 시 관리자 계정 자동 생성, 이미 존재하면 중복 생성하지 않음.

## Acceptance Criteria

- 위 Functional Scenarios가 모두 테스트로 존재하고 통과한다.
- `SecurityConfig.kt`에 더 이상 `permitAll` 전체 허용이 없다.
- `USER_ROLES.md` 3.1과 실제 구현된 권한 매핑이 1:1로 대응한다(서술 없는 권한을 임의로 허용하지 않았는지 확인).
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `ADR-004`가 `Accepted`로 전환되었거나, 전환되지 않았다면 그 이유가 기록되어 있다.
- `ARCHITECTURE.md` Deferred Decision #3이 제거(또는 해소로 갱신)되었다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준. 추가로, 이 작업은 보안에 영향을 주는 변경이므로 `docs/DEVELOPMENT.md` §6에 따른 **독립된 세션/도구에서의 Review Agent 리뷰**가 특히 중요하다 — 아래 "Open Questions"와 인가 매트릭스 정확성을 중점적으로 확인한다.

## Open Questions

- **계정 생성 정책**: `REQUIREMENTS.md`에는 "사용자(계정) 관리" 자체가 기능 요구사항으로 명시되어 있지 않다(§3.1~3.7은 학생/반·수업/일정/출결/숙제/학습기록/공지만 다룬다). 로그인 기능이 성립하려면 계정이 최소 1개 이상 존재해야 하므로, 이 작업은 "관리자가 계정을 생성할 수 있어야 한다"는 운영상 최소 필요에 근거해 관리자 전용 계정 생성 API를 포함했다 — 새로운 비즈니스 정책을 만든 것이 아니라 이미 정의된 관리자 역할의 최소 필요 기능으로 간주했다. **이 판단이 맞는지 사람의 확인이 필요하다.**
- **로그인 식별자 필드명**: `loginId`로 가정했다(이메일 주소인지, 별도 아이디인지 등은 어느 문서에도 서술이 없다) — 리스크가 낮은 기술적 가정이므로 구현 시 확인 권장.
- 계정 비활성화(`UserStatus.INACTIVE`) 시 기존 세션을 즉시 무효화할지 여부는 이 작업에서 다루지 않는다 — 실제 요구가 생기면 별도로 결정한다.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-004-session-based-authentication.md`
