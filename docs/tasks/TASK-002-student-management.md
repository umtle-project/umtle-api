# TASK-002: 학생(Student) 관리 도메인 구현

## Status

In Review — PR 생성 대기

## 진행 상황 (다음 세션에서 이어가기)

- **브랜치**: `feature/TASK-002-student-management`, `origin`에 push 완료.
- **커밋**(5개, 순서대로):
  1. `chore: ignore local tool state directories`
  2. `feat: implement student management domain (TASK-002)`
  3. `docs: record TSID identifier decision and architecture updates` (ADR-002)
  4. `docs: add TASK-002 record and sync requirements reference`
  5. `docs: propose common not-found error handling and add decisions index` (ADR-003, DECISIONS.md)
- **PR**: 아직 생성되지 않음 — 이 세션에서 `gh`가 로그인되어 있지 않아 자동 생성에 실패했다. 다음 세션에서:
  1. `gh auth login`으로 로그인
  2. `gh pr create` 또는 `/pr-helper`로 PR 생성. 제목/본문 초안은 아래 참고.
  3. `docs/DEVELOPMENT.md` §6에 따라 **독립된 세션/도구**에서 Review Agent 리뷰 진행 — 특히 `docs/adr/ADR-003-common-not-found-error-handling.md`(Proposed, 채택 여부 미결정)와 이번에 되돌린 soft delete 제거 판단을 중점 확인.
  4. 사람이 리뷰 결과를 보고 최종 승인/머지.
- **PR 제목 초안**: `feat: 학생(Student) 관리 도메인 구현 및 아키텍처 정리 (TASK-002)`
- **PR 본문 초안**: `.github/pull_request_template.md` 형식으로 이미 한 번 작성해 대화에 남겨뒀다 — 다음 세션에서 이 TASK 문서와 커밋 로그를 참고해 동일하게 재구성 가능.
- **머지 이후 다음 작업 후보**:
  - `ADR-003` 채택 여부 결정 → 채택되면 `StudentNotFoundException`/`StudentExceptionHandler`를 공통 패턴(`common.domain`/`common.presentation`)으로 마이그레이션.
  - `TASK-003`: 반/수업(Class/Lesson) 도메인 — `DOMAIN_MODEL.md`가 Student 다음으로 참조하는 도메인. 단 `ARCHITECTURE.md` Deferred Decision #1(Class/Lesson이 하나의 Aggregate인지)이 먼저 정리되어야 스키마를 확정할 수 있다.
  - `ARCHITECTURE.md` Deferred Decision #3(인증/인가 구현 방식) — `USER_ROLES.md`는 나왔지만 구현 방식 자체는 여전히 미정.

## Purpose

`PRD.md`/`REQUIREMENTS.md`의 1순위 핵심 기능이자 `DOMAIN_MODEL.md`가 다른 모든 도메인이 참조하는 중심 엔티티로 지목한 학생(Student)을 실제로 구현한다. 저장소의 첫 번째 비즈니스 도메인이다.

## Background

`TASK-001`로 `USER_ROLES.md`가 학생 관리 권한(관리자: 등록/조회/수정/비활성화, 선생님: 담당 학생 조회)을 정형화했다. 이 작업은 `ARCHITECTURE.md`가 "구현 시점에 결정"이라 명시한 패키지 구조와, `TASK-000`이 "첫 마이그레이션을 추가하는 작업에서 결정"하도록 미뤄둔 `hibernate.ddl-auto` 값도 함께 확정한다. ID 타입 정책(TSID)은 `docs/adr/ADR-002-tsid-as-identifier.md`로 별도 기록했다.

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.1 학생 관리
- `docs/DOMAIN_MODEL.md` 3.2 학생(Student)
- `docs/USER_ROLES.md` 3.1 학생 관리
- `docs/adr/ADR-002-tsid-as-identifier.md`

## Users and Permissions

`USER_ROLES.md` 3.1에 따라 관리자는 등록/조회/수정/비활성화, 선생님은 담당 학생 조회 권한을 가진다. 단, 이 작업은 인증/인가를 강제하지 않는다 — `ARCHITECTURE.md` Deferred Decision #3(인증/인가 구현 방식)이 아직 미정이라 API는 인증 없이 동작하며, 권한 매트릭스는 문서 수준에서만 반영되어 있다. "담당 학생" 범위 필터링도 반/수업 배정 도메인이 없어 구현하지 않았다 — 조회 API는 전체 학생을 반환한다.

## Preconditions

`TASK-000`(부트스트랩), `TASK-001`(USER_ROLES.md)이 완료되어 있어야 한다.

## Scope

- Student 도메인 모델, Repository 포트, Application Service, JPA Entity/Repository/Adapter, REST Controller/DTO/예외 처리.
- 최소 필드(`name`, `status`)만 다루는 등록/조회/목록/수정/비활성화 CRUD.
- 첫 Flyway 마이그레이션(`V1__create_students_table.sql`) 및 `hibernate.ddl-auto: validate` 설정.
- ID 타입을 TSID로 확정(`ADR-002`)하고 `com.github.f4b6a3:tsid-creator` 의존성 추가.
- 리뷰 피드백 반영: 모든 향후 Aggregate가 공유할 `BaseEntity`(`createdAt`/`updatedAt`, JPA Auditing)를 `common/infrastructure`에 도입하고 `StudentJpaEntity`가 이를 상속. API 경로에 `/api/v1` 접두사 적용.
- 아키텍처 재검토(2차): `BaseEntity`에 처음 추가했던 `deletedAt`/`@SQLDelete`/`@SQLRestriction`(soft delete)을 제거했다. 이유:
  - `REQUIREMENTS.md`를 전수 확인한 결과 "삭제"는 일정(§3.3)·숙제(§3.5)에만 등장하고, 학생은 시종일관 "비활성화"만 요구한다 — Student에는 삭제 개념 자체가 없다.
  - `status = INACTIVE`(비즈니스적 비활성화)와 `deletedAt != null`(기술적 소프트 삭제)이 같은 Aggregate에 공존하면 의미가 겹치거나 충돌할 수 있다(예: `status = ACTIVE`인데 `deletedAt`이 채워진 모순 상태가 코드상 방지되지 않음).
  - `StudentRepository` 포트에 애초에 `delete` 메서드가 없어 실제로 소비하는 경로가 없는 채로 인프라만 선반영된 상태였다 — `AGENTS.md`/`ARCHITECTURE.md`의 "불필요한 추상화 지양" 원칙에 어긋난다.
  - 실제 삭제가 필요한 Aggregate(예: 향후 일정, 숙제)가 생기면, 그 Aggregate에 한해 소프트/하드 삭제 여부를 그때 다시 결정한다 — `BaseEntity`에 미리 공통으로 넣지 않는다.
  - `createdAt`/`updatedAt`은 의미 충돌이 없고 거의 모든 테이블에 보편적으로 유용해 그대로 유지한다.

## Out of Scope

- Spring Security 기반 인증/인가 강제.
- "담당 학생" 스코핑(반/수업 배정 도메인 부재로 불가).
- 학부모 연결, 반/수업/출결/숙제/학습기록 연동.
- 프로젝트 전역 공통 예외 처리 구조(Deferred Decision #4) — 이 작업은 Student 도메인 한정 404/400 처리만 다룬다. 이후 `ADR-003`에서 부분적으로 다뤘다(Not Found 케이스만, Proposed).
- `name` 외 추가 필드(연락처, 생년월일 등) — 필요해지면 별도 마이그레이션/작업으로 추가.

## Functional Scenarios

- 관리자가 이름을 입력해 학생을 등록하면 상태 `ACTIVE`로 생성된다.
- 등록된 학생을 id로 조회하거나 전체 목록을 조회할 수 있다.
- 학생 이름을 수정할 수 있다.
- 학생을 비활성화하면 상태가 `INACTIVE`로 바뀌며, 이미 `INACTIVE`인 학생을 다시 비활성화해도 오류 없이 멱등하게 처리된다.
- 존재하지 않는 id를 조회/수정/비활성화하면 404를 반환한다.

## Business Rules

- 학생 이름은 공백일 수 없고 100자를 초과할 수 없다(`Student.register`, `Student.rename`에서 강제).
- 비활성화는 멱등하다 — `REQUIREMENTS.md`/`DOMAIN_MODEL.md`에 재비활성화 시 오류를 요구하는 서술이 없어 가장 단순한 규칙으로 채택한 구현 가정이다.

## API Changes

- `POST /api/v1/students` — 201, body `{ "name": string }` → `StudentResponse`
- `GET /api/v1/students` — 200, `StudentResponse[]`
- `GET /api/v1/students/{id}` — 200 / 404
- `PATCH /api/v1/students/{id}` — 200, body `{ "name": string }` → `StudentResponse`
- `POST /api/v1/students/{id}/deactivate` — 200 → `StudentResponse`(상태 `INACTIVE`)
- `StudentResponse`: `{ "id": number, "name": string, "status": "ACTIVE" | "INACTIVE" }`

## Domain Impact

`DOMAIN_MODEL.md` 3.2 학생(Student) Aggregate를 도입한다. 다른 Aggregate를 참조하지 않는다(아직 반/수업/출결/숙제/학습기록 미구현).

## Database Impact

`V1__create_students_table.sql` — `students` 테이블 신규 생성(`id BIGINT PK`, `name VARCHAR(100)`, `status VARCHAR(20)`, `created_at`/`updated_at DATETIME(6) NOT NULL` — `BaseEntity` 공통 컬럼). `deleted_at`(소프트 삭제)은 도입 후 재검토 과정에서 제거했다 — 위 Scope 항목의 "아키텍처 재검토(2차)" 참고.

## Exception and Error Handling

`StudentNotFoundException` → `StudentExceptionHandler`(Student 도메인 한정 `@RestControllerAdvice`)가 404 `ProblemDetail`로 변환. 입력값 검증 실패(`@NotBlank`, `@Size(max = 100)`)는 Spring 기본 처리로 400을 반환한다.

## Test Scenarios

- `StudentTest`(도메인 단위): 등록 시 ACTIVE 상태, 공백/100자 초과 이름 거부, 비활성화 전이, 비활성화 멱등성.
- `StudentApiTests`(MockMvc + Testcontainers 통합): 등록→조회→목록→수정→비활성화 전체 플로우, 존재하지 않는 id 404, 공백 이름 등록 400, 100자 초과 이름 등록/수정 400.

## Acceptance Criteria

- 위 API 5종이 명세대로 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다(Docker 필요).
- `docs/adr/ADR-002-tsid-as-identifier.md`가 존재하고 `ARCHITECTURE.md` Deferred Decision #4가 제거되었다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준. 추가로 이 작업 고유의 항목은 없다.

## Open Questions

- "담당 학생" 배정 기준, 학부모-학생 연결 방식, 인증/인가 구현 방식 — 모두 `USER_ROLES.md`/`DOMAIN_MODEL.md`/`ARCHITECTURE.md`에 이미 미정으로 명시되어 있으며 이 작업에서 해결하지 않는다.
- 추가 학생 필드(연락처 등) 필요 여부 — 실제 요구사항이 생기면 별도로 결정한다.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
