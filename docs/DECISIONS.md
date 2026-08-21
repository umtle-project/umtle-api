# DECISIONS.md

# 움틀 채택된 기술 결정 요약

이 문서는 `docs/adr/`에 기록된 ADR 중 **채택(Accepted)된** 결정만 한 줄 요약으로 모아두는 인덱스다. 결정의 배경·대안·트레이드오프 등 상세 내용은 각 ADR 본문을 따른다 — 이 문서는 내용을 중복 기록하지 않는다.

`Proposed`(아직 사람이 승인하지 않은) 상태의 ADR은 여기 올리지 않는다 — `docs/adr/`에서 직접 확인한다.

문서 우선순위는 `AGENTS.md`를 따른다.

---

| ADR | 제목 | 상태 | 날짜 |
|-----|------|------|------|
| [ADR-001](adr/ADR-001-start-with-monolith.md) | 단일 모놀리스로 시작한다 | Accepted | 2026-08-05 |
| [ADR-002](adr/ADR-002-tsid-as-identifier.md) | Aggregate 식별자로 TSID를 사용한다 | Accepted | 2026-08-09 |
| [ADR-003](adr/ADR-003-common-not-found-error-handling.md) | 공통 "Not Found" 예외 처리만 우선 통일한다 | Accepted | 2026-08-09 |
| [ADR-004](adr/ADR-004-session-based-authentication.md) | 세션 기반 인증과 Presentation 계층 인가를 채택한다 | Accepted | 2026-08-10 |
| [ADR-005](adr/ADR-005-query-strategy.md) | JPA 연관관계를 배제하고 ID 기반 QueryDSL 조회 전략을 채택한다 | Accepted | 2026-08-10 |
| [ADR-006](adr/ADR-006-class-lesson-aggregate-boundary.md) | Class와 Lesson을 별도 Aggregate로 분리하고 배정은 순수 id 값으로 관리한다 | Accepted | 2026-08-10 |
| [ADR-007](adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md) | 출결/숙제/학습 기록을 각각 독립 Aggregate로 분리하고 Lesson/Student는 id로만 참조한다 | Accepted | 2026-08-11 |
| [ADR-008](adr/ADR-008-user-student-parent-connection.md) | 학생 계정은 선택적 1:1, 학부모-학생 연결은 다대다로 관리한다 | Accepted | 2026-08-12 |
| [ADR-009](adr/ADR-009-user-signup-and-approval.md) | 사용자 자가 회원가입과 역할별 승인 절차를 도입한다 | Accepted | 2026-08-14 |
| [ADR-010](adr/ADR-010-request-tracing-and-logging.md) | 요청 단위 traceId와 접근/쿼리 로깅 기본 방침을 도입한다 | Accepted | 2026-08-14 |
| [ADR-013](adr/ADR-013-teacher-admin-parity-for-student-parent-domain.md) | 학생/학부모-연결 도메인에서 선생님과 관리자의 권한을 동등화한다 | Accepted | 2026-08-21 |
| [ADR-014](adr/ADR-014-teacher-classroom-scope.md) | 반-선생님 배정을 1:N으로 정정하고 선생님의 출결/숙제/학습 기록 쓰기 권한을 담당 학생으로 제한한다 | Accepted | 2026-08-21 |

---

새 ADR이 `Accepted`로 전환되면 이 표에 한 줄을 추가한다. `Superseded`로 바뀌면 상태 열을 갱신하되 행을 삭제하지 않는다.
