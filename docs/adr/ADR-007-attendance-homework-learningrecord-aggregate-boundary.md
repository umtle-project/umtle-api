# ADR-007: 출결(Attendance)/숙제(Homework)/학습 기록(Learning Record)을 각각 독립 Aggregate로 분리하고, Lesson/Student는 id로만 참조한다

## Status

Accepted (2026-08-11)

이 ADR은 사용자가 세션 중 직접 결정했다(`ADR-002`, `ADR-006`과 동일한 방식) — 세 가지 질문(Aggregate 경계, 출결의 참조 구조, 숙제의 참조 구조)에 대해 사용자가 그 자리에서 답했다.

## Date

2026-08-11

## Context

- `ARCHITECTURE.md` §8 Deferred Decision #1("Aggregate 내부 경계 세부 확정")은 "Attendance/Homework/LearningRecord가 Lesson 또는 Student 중 어디에 종속되는 Aggregate인지"를 근거 부족으로 보류해왔다. Class/Lesson 경계와 반-학생/반-선생님 배정 방식은 `ADR-006`으로 이미 해소되어 있었다.
- `TASK-005`(Class/Lesson)가 완료되어, 다음 순서로 출결/숙제/학습 기록 도메인을 구현하려면 이 경계를 확정해야 한다(`docs/tasks/TASK-005-class-lesson-management.md`의 Acceptance Criteria가 이 부분 미해소를 명시).
- `DOMAIN_MODEL.md` §4 도메인 간 관계도는 이미 방향성을 암시하고 있었다:
  - `수업(Lesson) ── 기준 ──> 출결(Attendance) ── 대상 ──> 학생`
  - `수업(Lesson) 또는 학생 ── 부여 ──> 숙제(Homework)`
  - `학생 ── 누적 ──> 학습 기록(Learning Record)`
- `REQUIREMENTS.md` 3.4~3.6은 출결을 "수업별 학생 출결 상태", 숙제를 "수업 또는 학생 단위로 부여", 학습 기록을 "학생별 학습 기록"으로 각각 명시한다.
- 출결 상태값, 숙제 상태값, 학습 기록의 학생/학부모 공개 범위 등 세부 내용은 `REQUIREMENTS.md` §5와 `DOMAIN_MODEL.md` §6에서 여전히 미정으로 남아 있으며, 이 ADR은 그 부분을 다루지 않는다 — Aggregate 경계와 id 참조 구조만 다룬다.
- `ADR-006`에서 확립된 원칙(무한정 늘어나는 자식 컬렉션을 하나의 Aggregate에 두지 않는다, 다른 Aggregate는 순수 id 값으로만 참조한다)이 이번 결정에도 그대로 적용 가능한 선례로 존재한다.

## Decision

이번 ADR은 Deferred Decision #1을 **완전히** 해소한다.

1. **Attendance, Homework, LearningRecord는 각각 독립된 Aggregate Root로 분리한다.**
   - 세 도메인 모두 Lesson이나 Student의 하위 엔티티/컬렉션으로 두지 않는다.
   - `Lesson`, `Student`를 참조할 때는 객체 참조가 아닌 `lessonId: Long`, `studentId: Long` 값만 컬럼으로 보유한다(`ARCHITECTURE.md` §6.1 원칙 그대로 적용).
   - 각 Aggregate는 독립적인 트랜잭션 경계를 가진다.
2. **Attendance는 `lessonId`와 `studentId`를 모두 필수로 참조한다.**
   - 레코드 1개 = 특정 수업(Lesson) x 특정 학생(Student)의 출결 상태.
   - `studentId`는 해당 수업이 속한 `Classroom`에 배정된 학생인지 여부를 Application Service에서 검증한다(물리 FK 없음, `ARCHITECTURE.md` §6.1).
   - 출결 상태값의 구체적인 종류(출석/지각/결석 등)는 이 ADR의 범위 밖이며, `TASK-006`에서 정의한다.
3. **Homework는 `studentId`를 필수로, `lessonId`는 nullable로 참조한다.**
   - 모든 Homework는 특정 학생에게 부여된 것으로 취급한다(`studentId` 필수) — `REQUIREMENTS.md` 3.5의 "학생은 본인에게 부여된 숙제를 조회"와 직접 대응.
   - 수업에서 비롯된 숙제인 경우 `lessonId`를 함께 기록하고, 그렇지 않은 경우(학생 단위로 직접 부여) `lessonId`는 null로 둔다.
   - "수업 단위 숙제 템플릿"(하나의 숙제를 여러 학생에게 일괄 부여) 같은 별도 개념은 이번 ADR에서 도입하지 않는다 — 현재 요구사항에 근거가 없다.
   - 제출/수행 상태의 구체적인 모델링(Homework 내부 필드 vs 별도 Submission 개념)과 상태값 종류는 이 ADR의 범위 밖이며, `TASK-007`에서 정의한다.
4. **LearningRecord는 `studentId`만 참조한다.**
   - `lessonId`는 참조하지 않는다 — `REQUIREMENTS.md` 3.6과 `DOMAIN_MODEL.md` §4는 학습 기록을 수업이 아닌 학생 단위로 누적되는 정보로 정의한다.
   - 시간 순으로 계속 누적되는 형태이므로, 개별 레코드가 독립적으로 생성되고 Student 전체를 로딩하지 않고도 조회 가능해야 한다.

## Decision Drivers

- 세 도메인 모두 시간이 지날수록 레코드 수가 무한정 늘어난다(수업마다 출결, 학생마다 숙제/학습 기록이 계속 쌓임) — `ADR-006`이 Lesson에 적용한 것과 동일한 이유로, Lesson/Student 내부에 자식 컬렉션으로 두면 로딩/트랜잭션 비용이 계속 커진다.
- `DOMAIN_MODEL.md` §4의 관계도가 이미 이 구조(수업 기준 출결, 수업-또는-학생 기준 숙제, 학생 기준 학습 기록)를 명시하고 있어 별도 근거를 새로 만들 필요가 없다.
- `ARCHITECTURE.md` §6.1이 이미 "Aggregate 간에는 id로만 참조"를 프로젝트 전역 원칙으로 확정해두었다.
- `ADR-005`가 "여러 Aggregate에 걸친 조회는 QueryDSL + id 기반 JOIN"을 정책으로 확정했으므로, Attendance/Homework/LearningRecord를 분리해도 "이 수업의 출결 전체", "이 학생의 숙제 전체" 같은 화면성 조회는 이미 준비된 방식으로 처리 가능하다.
- 숙제의 "수업 또는 학생 단위" 요구사항(`REQUIREMENTS.md` 3.5)을 만족하려면 최소 하나의 nullable 참조가 필요한데, `studentId`를 필수로 고정하면 "학생이 본인 숙제를 조회"라는 가장 빈번한 조회 패턴이 항상 단순하게 유지된다.

## Considered Alternatives

### 1. Attendance를 Lesson의 하위 엔티티/컬렉션으로 편입

- 설명: `Lesson` Aggregate가 `attendances: List<Attendance>`를 직접 보유.
- 기각 사유: Lesson 하나당 배정된 학생 수만큼 레코드가 생기고, 향후 Lesson을 조회할 때마다 출결까지 함께 로딩되거나 그래프 탐색 위험이 생긴다. `ADR-006`이 이미 "Lesson 자체도 Class의 무한정 자식으로 두지 않는다"고 결정한 것과 같은 이유로 일관성이 깨진다.

### 2. LearningRecord/Homework를 Student의 하위 엔티티/컬렉션으로 편입

- 설명: `Student` Aggregate가 `learningRecords`, `homeworks`를 직접 보유.
- 기각 사유: 학습 기록은 "지속적으로 누적"되는 정보로 명시되어 있어(`REQUIREMENTS.md` 3.6) 시간이 지날수록 무한정 늘어난다. Student 조회마다 전체 이력을 로딩할 근거가 없다.

### 3. Homework를 "수업 단위 템플릿 + 학생별 배정/제출"의 두 개념으로 분리

- 설명: 수업 전체에 부여되는 숙제 템플릿과, 그 템플릿이 개별 학생에게 배정된 결과(제출 상태 포함)를 별도 Aggregate로 나눈다.
- 기각 사유(현재는): `REQUIREMENTS.md` 3.5에는 "일괄 템플릿"이라는 개념이 명시되어 있지 않다 — 현재 요구사항은 "수업 또는 학생 단위로 부여"만 요구하며, 템플릿/배정 분리는 근거 없이 미리 만드는 과설계다. 실제로 "한 숙제를 여러 학생에게 동시에 부여"하는 요구가 명확해지면 그때 재검토한다.

### 4. (채택) 독립 Aggregate + Attendance(lessonId+studentId 필수) / Homework(studentId 필수, lessonId nullable) / LearningRecord(studentId만)

- 설명: 위 Decision 참고.
- 기각 사유 없음 — 이 ADR의 결정.

## Consequences

### Positive

- Lesson/Student Aggregate의 로딩 비용에 영향을 주지 않으면서 출결/숙제/학습 기록 레코드가 무한정 늘어날 수 있다.
- `TASK-006`(출결), `TASK-007`(숙제), `TASK-008`(학습 기록)의 스코프가 명확해져 각 TASK 문서 작성 시 "Domain Impact" 섹션을 바로 채울 수 있다.
- 참조 방식이 `ADR-006`(Class/Lesson)과 동일한 패턴이라 코드 일관성이 유지된다.

### Negative

- "이 수업의 출결 전체 조회", "이 학생의 숙제/학습 기록 전체 조회" 같은 화면성 조회는 각 Aggregate를 별도로 조회해 Application Service에서 조합해야 한다(`ADR-005`의 QueryDSL id 기반 조회로 처리).
- Homework의 `lessonId` nullable 컬럼은 값이 없는 경우("학생 단위로 직접 부여")와 있는 경우("수업에서 비롯")를 애플리케이션 로직에서 항상 구분해서 다뤄야 한다.

### Risks

- "한 숙제를 여러 학생에게 일괄 부여"하는 요구사항이 실제로 생기면, 현재의 "Homework 1건 = 학생 1명" 구조로는 표현력이 부족해 템플릿/배정 분리(대안 3) 재검토가 필요하다.
- 출결이 "수업에 배정된 학생만" 대상이 되는지 여부는 이 ADR이 아닌 Application Service 검증 규칙으로 남아 있다 — `TASK-006`에서 구체화되지 않으면 배정되지 않은 학생의 출결도 생성 가능한 상태로 구현될 위험이 있다.

## Validation

- 숙제를 여러 학생에게 일괄 부여해야 하는 요구사항이 실제로 확정되는 시점에 Homework의 템플릿/배정 분리 여부를 재검토한다.
- `TASK-006`/`TASK-007`/`TASK-008` 구현 중 이 구조와 맞지 않는 요구사항이 발견되면 코드를 먼저 바꾸지 않고 이 ADR의 갱신 여부를 먼저 논의한다(`ARCHITECTURE.md` §9 변경 원칙과 동일).

## Related Documents

- `docs/ARCHITECTURE.md` §8 Deferred Decision #1 (이 ADR로 완전 해소)
- `docs/ARCHITECTURE.md` §6.1 (Aggregate 간 id 참조 원칙)
- `docs/adr/ADR-005-query-strategy.md` (id 기반 QueryDSL 조회 전략)
- `docs/adr/ADR-006-class-lesson-aggregate-boundary.md` (동일 패턴의 선례)
- `docs/DOMAIN_MODEL.md` §3.5~3.7, §4
- `docs/tasks/TASK-005-class-lesson-management.md`
