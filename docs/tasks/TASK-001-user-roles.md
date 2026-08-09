# TASK-001: USER_ROLES.md 초안 작성

## Status

Draft

## Purpose

`AGENTS.md`의 Documentation Priority에서 세 번째로 우선순위가 높은 `docs/USER_ROLES.md`가 아직 작성되지 않아, 향후 어떤 도메인 기능을 구현하든 권한을 임시로 판단해야 하는 선행 병목을 해소한다.

## Background

`DOMAIN_MODEL.md`(3.1, 6장), `REQUIREMENTS.md`(2장, 5장), `ARCHITECTURE.md`(도입부, Deferred Decision #3)가 모두 `USER_ROLES.md`를 "미작성"으로 참조하며 권한/인가 관련 결정을 이 문서로 미루고 있었다.

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 2장(사용자), 3장(기능 요구사항), 5장(아직 결정되지 않은 사항)
- `docs/DOMAIN_MODEL.md` 3.1(사용자 도메인), 6장(아직 결정되지 않은 사항)
- `docs/ARCHITECTURE.md` 도입부, 8장 Deferred Decisions #3

## Users and Permissions

이 작업 자체가 `docs/USER_ROLES.md`를 만드는 작업이다. 작성 원칙: `REQUIREMENTS.md` 3장에 이미 서술된 역할별 권한만 정형화하고, 서술이 없는 권한은 새로 정하지 않고 "아직 결정되지 않은 사항"으로 명시한다 (`AGENTS.md`의 "권한에 영향을 주는 모호함은 임의로 정하지 않는다" 원칙).

## Scope

- `docs/USER_ROLES.md` 신규 작성 — 역할 정의, 도메인(3.1~3.7) × 역할 권한 매트릭스, 아직 결정되지 않은 사항 목록.
- `docs/DOMAIN_MODEL.md`, `docs/ARCHITECTURE.md`, `docs/REQUIREMENTS.md`에서 "`USER_ROLES.md`(미작성)"을 참조하던 문구를 사실에 맞게 최소 수정.

## Out of Scope

- ID 타입 정책, 에러 응답 규격 등 `ARCHITECTURE.md` 8장의 다른 Deferred Decisions.
- 인증/인가의 구체적 구현 방식(세션 vs 토큰 등, Deferred Decision #3).
- `USER_ROLES.md`에 "아직 결정되지 않은 사항"으로 남긴 항목의 실제 정책 결정.
- 코드/스키마 변경 (문서 전용 작업).

## Acceptance Criteria

- `docs/USER_ROLES.md`가 존재하며, 모든 권한 서술이 `docs/REQUIREMENTS.md` 3장의 문장과 1:1로 대응한다(새로 지어낸 권한 없음).
- `docs/REQUIREMENTS.md` 3장에 서술이 없는 권한 항목은 `docs/USER_ROLES.md` 4장에 미정으로 명시되어 있다.
- `docs/DOMAIN_MODEL.md`, `docs/ARCHITECTURE.md`, `docs/REQUIREMENTS.md`의 "`USER_ROLES.md`(미작성)" 관련 문구가 갱신되었다.

## Definition of Done

`docs/DEVELOPMENT.md` § Definition of Done 기준. 코드 변경이 없으므로 Gradle 테스트 실행은 해당 없음.

## Open Questions

`docs/USER_ROLES.md` 4장 "아직 결정되지 않은 사항"에 나열된 항목 전체(학생 관리에서 학생·학부모 권한, 반/수업 관리 조회 권한, 숙제 관리에서 관리자 권한, 학습 기록 공개 범위, 공지 채널/발송정책, "담당" 배정 기준, 학부모-학생 연결 방식, 역할 간 위계, 인가 기술 구현 방식) — 이 작업에서 해결하지 않는다.

## Related ADRs

없음.
