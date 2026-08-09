# DEVELOPMENT.md

# 움틀 개발 프로세스

이 문서는 움틀(Umtle) 프로젝트의 Git/PR 프로세스와, `AGENTS.md`에서 정의한 AI Role(Planning / Architecture /
Implementation / Review / Refactoring Agent)이 실제 개발 흐름에서 어떻게 결합되는지를 정의한다.

문서 우선순위는 `AGENTS.md`를 따르며, 이 문서는 `AGENTS.md`의 원칙과 모순되지 않는 범위에서만 유효하다.

Bootstrap(프로젝트 초기 세팅) 단계는 완료되었다. Bootstrap은 예외적으로 `main` 브랜치에서 직접 진행했으며, 그 이후부터는 아래 Git 관련 절차(브랜치
전략, 커밋 컨벤션, PR 프로세스)가 실제로 적용되는 표준 프로세스다.

혼자 개발하는 프로젝트를 기준으로, 과도한 프로세스보다는 AI Role 간 독립성(구현자가 자신의 결과를 승인하지 않음)을 지키는 데 필요한 최소한의 절차만 정의한다.

---

## 1. Development Principles

- `AGENTS.md`의 Development Principles(단순한 구조 우선, 과도한 추상화 지양, 도메인 중심 설계 등)를 그대로 따른다.
- 이 문서는 그 원칙을 "코드를 어떻게 짤 것인가"가 아니라 "변경을 어떤 절차로 저장소에 반영할 것인가"의 관점에서 구체화한다.
- 프로세스 자체도 단순함을 우선한다 — 솔로 개발자에게 불필요한 승인 단계나 브랜치 계층을 추가하지 않는다.

---

## 2. Branch Strategy

- `main` 브랜치 하나만 항상 배포 가능한 상태로 유지한다.
- 별도의 `develop` 브랜치는 두지 않는다 (팀 규모와 배포 빈도상 근거가 없다 — `docs/adr/ADR-001-start-with-monolith.md`와 동일한 판단
  기준).
- 모든 변경은 `main`에서 분기한 짧은 수명의 브랜치에서 작업한다.
    - `feature/<slug>` — 새 기능
    - `fix/<slug>` — 버그 수정
    - `docs/<slug>` — 문서 변경
    - `chore/<slug>` — 빌드/설정 등 기타 변경
- 관련 Task 문서(`docs/tasks/TASK-XXX-*.md`)가 있는 작업은 `<slug>`에 해당 Task ID를 포함한다 (예:
  `feature/TASK-001-create-academy`, `fix/TASK-015-fix-login`). Task 문서 없이 진행하는 작업(7장 참고)은 짧은 설명만으로
  충분하다 (예: `chore/add-spotless`, `docs/update-readme`).
- 브랜치는 병합 후 삭제한다.

---

## 3. Git Workflow

Bootstrap 이후부터는 `main`에서 직접 작업하지 않는다. `main`에는 직접 commit하거나 push하지 않으며, 모든 변경은 PR을 통해서만 `main`에
반영된다.

1. 모든 개발은 GitHub Issue로 시작한다.
2. Issue를 기반으로 2장의 브랜치 명명 규칙에 따라 `main`에서 새 브랜치를 생성한다.
3. 작업 범위는 관련 `docs/tasks/` 문서(있는 경우) 또는 Issue에 기술된 범위로 한정한다.
4. 의미 있는 단위로 작게 커밋한다 (4장 Commit Convention 참고).
5. 브랜치를 원격에 푸시하고, 관련 Issue와 Task 문서를 연결한 PR을 생성한다 (5장 Pull Request Process 참고).
6. Review Agent의 독립 리뷰를 거친 뒤, 사람이 최종적으로 머지를 결정한다.
7. 머지 후 브랜치를 삭제한다.

---

## 4. Commit Convention

Conventional Commits 스타일을 따른다.

```
<type>: <short summary>
```

- `feat`: 새 기능
- `fix`: 버그 수정
- `docs`: 문서 변경
- `refactor`: 동작 변경 없는 구조 개선
- `test`: 테스트 추가/수정
- `chore`: 빌드, 설정, 의존성 등

원격 저장소에 관련 GitHub Issue가 있으면 커밋 메시지 끝에 `(#이슈번호)`를 추가한다.

```
<type>: <short summary> (#<issue-number>)
```

예: `feat: add student enrollment flow (#123)`. 관련 Issue가 없는 커밋(예: Bootstrap 단계의 초기 커밋)은 생략한다.

커밋 메시지는 영어로 작성하며, "무엇을" 바꿨는지보다 "왜" 바꿨는지를 우선 설명한다.

하나의 커밋은 하나의 목적만 가진다.

- 서로 다른 성격의 변경은 같은 커밋에 포함하지 않는다.
- 문서, 설정, 기능 구현은 가능하면 서로 다른 커밋으로 분리한다.

---s

## 5. Pull Request Process

- 모든 변경(문서 포함)은 `main`에 직접 커밋하지 않고 PR을 통해 반영한다 — 솔로 개발자라도 PR은 Review Agent가 개입할 수 있는 체크포인트 역할을 한다.
- PR 제목은 한국어로 작성한다.
- PR은 반드시 관련 GitHub Issue와 Task 문서(`docs/tasks/TASK-XXX-*.md`, 있는 경우)를 연결한다.
- PR 설명에는 다음을 포함한다.
    - 변경 목적과 관련 Issue/`docs/tasks/` 항목 또는 요청 내용
    - 영향을 받는 문서(있는 경우) 및 갱신 여부
    - 실행한 테스트/검증 결과
- PR은 Review Agent의 리뷰(6장 참고)를 거친 뒤 사람이 최종 승인하고 머지한다. Implementation Agent가 자신의 PR을 스스로 승인하지 않는다.
- Merge 방식은 Squash Merge를 사용한다 — 브랜치 내 커밋 수와 무관하게 `main`의 히스토리를 PR 단위로 단순하게 유지한다.

---

## 6. AI Collaboration Workflow

`AGENTS.md`의 Role이 개발 흐름에서 다음과 같이 이어진다.

1. **Planning Agent**: 요청을 분석해 작업 범위와 완료 기준을 정리한다 (7장 Task Workflow 참고).
2. **Architecture Agent** (필요한 경우만): 구조적 결정이 필요하면 `ARCHITECTURE.md`/`DOMAIN_MODEL.md`와의 정합성을 검토하고
   제안(ADR 포함)을 작성한다. 채택 여부는 사람이 결정한다.
3. **Implementation Agent**: `docs/prompts/IMPLEMENT_TASK.md`의 프롬프트로 브랜치를 생성해 범위 내에서 구현하고 테스트를
   작성/실행한 뒤 PR을 연다.
4. **Review Agent**: `docs/prompts/REVIEW_CHANGE.md`의 프롬프트로, 구현을 수행한 것과 다른 세션/컨텍스트(가능하면 다른 AI 도구)에서
   PR diff를 독립적으로 리뷰하고, 결과를 `docs/templates/REVIEW_TEMPLATE.md` 형식으로 `docs/reviews/`에 기록한다.
5. **사람**: 리뷰 결과와 diff를 확인하고 최종 기술적 의사결정(승인/보완 요청/반려)을 내린 뒤 머지한다.

동일한 세션이 3단계와 4단계를 동시에 수행할 수 없다 — 이는 `AGENTS.md`의 "구현한 에이전트가 자신의 결과를 최종 승인하지 않는다" 원칙을 지키기 위한 절차적 장치다.

---

## 7. Task Workflow

- 작업 단위가 명확하지 않거나 여러 파일/도메인에 걸치는 경우, 시작 전에 `docs/templates/TASK_TEMPLATE.md`를 복사해
  `docs/tasks/TASK-XXX-<slug>.md`로 작업 문서를 둔다.
- 작업 문서에는 최소한 다음을 포함한다 (템플릿의 나머지 항목은 작업 규모에 맞게 생략 가능).
    - 작업 범위(무엇을 하고, 무엇을 하지 않는가)
    - 인수 조건과 Definition of Done
    - 관련 상위 문서(REQUIREMENTS.md 항목, ADR 등)
- 작은 범위의 변경(오타 수정, 단일 파일의 명확한 버그 수정 등)은 별도 task 문서 없이 진행할 수 있다.
- Task 문서 작성 후 GitHub Issue를 생성해 개발을 시작한다 (3장 Git Workflow 참고).

---

## 8. Code Review Workflow

- 리뷰는 `AGENTS.md`의 "Review Mode" 절차를 따른다.
    - 명시적으로 수정을 요청받지 않는 한 파일을 수정하지 않는다.
    - 요구사항, task 문서, 도메인 규칙, 아키텍처 결정 대비 변경을 검토한다.
    - 권한, 트랜잭션, 데이터 정합성, 동시성, 성능, 예외 처리, 테스트 누락을 확인한다.
    - 발견 사항을 Critical / Major / Minor로 분류하고, 파일 위치·실패 시나리오·권장 수정안을 포함한다.
    - 근거 없는 추측성 지적은 하지 않는다.
- 리뷰는 PR diff를 대상으로 하며, 구현을 수행한 세션/컨텍스트와 독립적으로 수행되어야 한다.
- `docs/prompts/REVIEW_CHANGE.md` 프롬프트로 리뷰를 시작하고, 결과는 `docs/templates/REVIEW_TEMPLATE.md` 형식으로
  `docs/reviews/`에 기록한다.

---

## 9. Testing Workflow

- `@SpringBootTest`가 포함된 테스트는 Testcontainers(`TestcontainersConfiguration`)를 통해 실제 MySQL 컨테이너에
  연결한다 — 로컬에서 테스트를 실행하려면 Docker가 실행 중이어야 한다. 자세한 내용은 `docs/tasks/TASK-000-bootstrap-project.md`를
  참고한다.
- 현재 `src/test`에는 컨텍스트 로딩 테스트와 Health Check 테스트만 존재하며, 실제 도메인 로직에 대한 테스트는 아직 없다.
- 도메인/애플리케이션 로직을 추가하는 모든 PR은 관련 단위 테스트를 함께 포함한다.
- PR을 열기 전 `./gradlew test`를 실행하고 결과를 PR 설명에 남긴다.
- 테스트를 실행할 수 없는 상황이면 그 이유를 명시한다 (`AGENTS.md` Validation 참고).

---

## 10. Documentation Update Rules

- `AGENTS.md`의 "Documentation Changes" 원칙을 따른다: 구현 편의를 위해 요구사항/비즈니스 규칙 문서를 사후에 고치지 않는다.
- 구현 중 문서와 다른 접근이 필요하다고 판단되면, 먼저 구현을 멈추고 충돌을 보고한 뒤 문서 갱신 여부를 논의한다.
- 변경이 영향을 미치는 문서(REQUIREMENTS.md, DOMAIN_MODEL.md, ARCHITECTURE.md, 관련 ADR 등)는 같은 PR 안에서 함께 갱신한다.

---

## 11. Definition of Done

다음 조건을 모두 만족할 때만 작업이 완료된 것으로 본다.

- 요청된 동작이 구현되었다.
- 관련 테스트가 추가/갱신되고 실행되었다.
- 영향을 받는 문서가 함께 갱신되었다.
- Implementation Agent가 아닌 독립된 Review Agent의 리뷰를 거쳤다.
- 사람이 최종적으로 확인하고 승인(머지)했다.
- 변경 파일 목록, 검증 결과, 남은 리스크가 보고되었다.

---

## 12. Documentation Conventions

- File names use kebab-case.
- Task: TASK-001-create-academy.md
- ADR: ADR-001-start-with-modular-monolith.md
- Review: REVIEW-TASK-001.md
- Templates: *_TEMPLATE.md

---

## 13. Configuration & Infrastructure Conventions

- Spring configuration files use the `application-<profile>.yml` naming pattern (`.yml`, not
  `.yaml`):
    - `application.yml` — only what's genuinely common across every profile (app name, which profile
      is active by default, and settings like Actuator health exposure that must behave identically
      everywhere).
    - `application-local.yml` / `application-test.yml` / `application-prod.yml` — everything else,
      split per profile. `local` and `test` currently need no datasource settings (Docker Compose /
      Testcontainers service connection wires it automatically); `prod` is currently an empty
      placeholder, not activated anywhere.
    - Spring configuration files should not need to declare datasource credentials directly where a
      service connection (Docker Compose, Testcontainers) can wire them automatically — see
      `docs/tasks/TASK-000-bootstrap-project.md`.
- Docker-related files (e.g. `docker/compose.yaml`) live under the `docker/` directory at the
  project root, not the repository root.
- `docker/compose.yaml` is local-development-only (never deployed) and currently has its MySQL
  credentials hardcoded directly rather than sourced from an env file — there is no `.env` in active
  use in this project right now. If a genuinely security-sensitive value needs to be introduced
  later (e.g. for something that gets deployed), keep it out of tracked files and document the
  convention here at that point rather than reusing this note as-is.
- See `docs/tasks/TASK-000-bootstrap-project.md` for the rationale behind the current profile setup
  and Docker Compose wiring.

---

## 14. Code Style

- Kotlin source (`src/**/*.kt`) and Gradle Kotlin DSL files (`*.gradle.kts`) are formatted
  with [Spotless](https://github.com/diffplug/spotless)
  using [ktlint](https://pinterest.github.io/ktlint/)'s standard rule set (4-space indentation,
  import ordering, no wildcard imports, trailing-whitespace/final-newline cleanup, among others).
  Configuration lives in `build.gradle.kts`.
- Before opening a PR, run:
    - `./gradlew spotlessApply` — auto-format any violations.
    - `./gradlew spotlessCheck` — verify formatting without changing files; this also runs
      automatically as part of `./gradlew check` / `./gradlew build`, so CI-equivalent local builds
      fail on style violations.
- Do not hand-tune formatting to work around ktlint (e.g. inline `// ktlint-disable` suppressions)
  without a concrete reason recorded in the PR description — prefer letting `spotlessApply` reformat
  the code.
- No custom rule overrides are configured yet; adopt ktlint's defaults as-is unless a real recurring
  friction point justifies deviating, and record that decision here if so.
