# TASK-013: 학생(Student) 관리 고도화 — 프로필 필드 확장 및 상세 조회

## Status

Merged — `docs/reviews/REVIEW-TASK-013-2026-08-21.md`의 Major 항목(프로필 PATCH 부분 수정 시 데이터 유실)이 Codex 구현 및 Claude Code 검증으로 해소됨. 사람이 PR #24를 `main`에 수동 머지 완료(2026-08-21).

## Purpose

`TASK-002`가 도입한 최소 `Student` 모델(`name`, `status`뿐)을 확장해, 선생님과 관리자가 학생을 더 풍부하게 관리·파악할 수 있도록 한다. (1) 연락처/생년월일/학교·학년/메모 등 실무에 필요한 프로필 필드를 추가하고, (2) 학생 한 명을 조회할 때 소속 반·출결 현황·숙제 현황을 한 번에 볼 수 있는 상세 조회 API를 신설한다.

## Background

`Student`(`TASK-002`)는 의도적으로 `name`/`status`만 가진 최소 모델로 시작했고("추가 필드는 필요해지면 별도 작업으로"), 이후 `Classroom`(`TASK-005`), `Attendance`(`TASK-006`), `Homework`(`TASK-007`)가 각각 독립 Aggregate로 구현되어 `studentId`로 `Student`를 참조하게 되었다(`ADR-007`). 그러나 학생 개별 화면에서 이 정보들을 한 번에 확인할 수 있는 조회는 아직 없고, `Student` 자체도 이름 외의 신상 정보를 담지 못해 실제 학원 운영에 쓰기에는 너무 단순하다는 것이 이번 작업의 배경이다.

이 작업은 `Student` Aggregate의 경계나 다른 Aggregate와의 참조 구조를 바꾸지 않는다 — `Student`에 필드를 추가하고, 이미 존재하는 `Classroom`/`Attendance`/`Homework`를 `ADR-005`가 확립한 "여러 Aggregate에 걸친 화면성 조회는 Application Service에서 개별 조회 후 조합" 패턴으로 묶어 보여주는 것뿐이다.

**[변경 이력]** 이 문서는 최초 작성 이후 한 차례 개정되었다: 최초 버전은 선생님을 "조회만"으로 제한했으나(당시 이 문서를 기준으로 Implementation Agent가 이미 코드/테스트를 구현함), 이후 세션에서 사용자가 "선생님도 학생·학부모 도메인에서 관리자와 동등한 권한을 가진다"고 직접 결정했다(`ADR-013`). 이번 개정은 그 결정을 반영해 Users and Permissions, 인가, Business Rules, Test Scenarios를 갱신한다 — **이미 구현된 코드는 이전 버전(선생님=조회만) 기준이므로, 아래 "구현 반영 필요" 절을 참고해 후속 구현이 필요하다.**

## Related Documents and Requirement IDs

- `docs/REQUIREMENTS.md` 3.1 학생 관리
- `docs/DOMAIN_MODEL.md` 3.2 학생(Student)
- `docs/USER_ROLES.md` 3.1 학생 관리
- `docs/tasks/TASK-002-student-management.md` — 이 작업이 확장하는 기존 `Student` 구현
- `docs/tasks/TASK-005-class-lesson-management.md`, `docs/tasks/TASK-006-attendance-management.md`, `docs/tasks/TASK-007-homework-management.md` — 상세 조회가 조합하는 기존 도메인
- `docs/adr/ADR-002-tsid-as-identifier.md`(ID 정책)
- `docs/adr/ADR-003-common-not-found-error-handling.md`(공통 404 처리)
- `docs/adr/ADR-005-query-strategy.md`(연관관계 금지, id 기반 화면성 조회 전략 — 이 작업의 상세 조회가 실제 적용 사례)
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`(Attendance/Homework가 `studentId`로만 참조되는 구조)
- `docs/adr/ADR-013-teacher-admin-parity-for-student-parent-domain.md`(선생님-관리자 권한 동등화 결정 — 이 개정판이 반영하는 근거)

## Users and Permissions

`ADR-013`에 따라 `USER_ROLES.md` 3.1이 개정되었다 — **선생님은 학생 도메인에서 관리자와 동등한 권한을 가진다**(이전 버전에서는 조회만 가능했다):

| 역할 | 권한 |
|------|------|
| 관리자 | 학생 등록/조회/수정(신규 프로필 포함)/비활성화 |
| 선생님 | 관리자와 동일 — 학생 등록/조회/수정(신규 프로필 포함)/비활성화. 신규 상세 조회(`GET .../detail`)와 프로필 수정(`PATCH .../profile`) 모두 관리자와 동등하게 허용한다(`ADR-013`) |
| 학생 | 서술 없음 — 이번 작업에서도 부여하지 않는다(변경 없음) |
| 학부모 | 서술 없음 — 이번 작업에서도 부여하지 않는다(변경 없음) |

"담당 학생" 스코핑(선생님이 자신이 속한 반의 학생만 보도록 제한)은 `DOMAIN_MODEL.md` §6/`USER_ROLES.md` §4에 여전히 미정으로 남아 있어, `TASK-006`/`TASK-007`과 동일하게 이번에도 스코핑 없이 전체 학생 대상 조회를 허용한다.

## Preconditions

`TASK-002`(Student), `TASK-005`(Classroom/Lesson), `TASK-006`(Attendance), `TASK-007`(Homework)가 모두 구현되어 있어야 한다.

## Scope

### Student 도메인 필드 확장

- `student/domain/Student.kt`에 다음 필드 추가(모두 nullable, 기본값 `null`):
  - `phone: String?` — 형식/정규식 검증 없음, 길이만 검증.
  - `birthDate: LocalDate?`
  - `school: String?`, `grade: String?` — 둘 다 자유 텍스트(초/중/고 등 학년 체계가 다양해 숫자·enum으로 강제하지 않는다).
  - `memo: String?` — 자유 텍스트.
- `Student.register(name)` 시그니처는 변경하지 않는다 — 신규 필드는 전부 `null`로 시작하며, 기존 회원가입 흐름(`TASK-011`의 학생 자가등록 포함)에 영향이 없다.
- 신규 도메인 메서드: `updateProfile(phone: String?, birthDate: LocalDate?, school: String?, grade: String?, memo: String?)`.
  - **프로필 섹션 전체를 통째로 교체한다** — 개별 필드 단위 partial update(일부 필드만 보내고 나머지는 유지)는 도입하지 않는다. 호출 시 5개 인자를 항상 전부 전달하며, 각 인자가 `null`이면 그 필드는 "값 없음"으로 저장된다.
  - 검증: `phone`은 20자 초과 불가, `school`은 100자 초과 불가, `grade`는 20자 초과 불가, `memo`는 1000자 초과 불가(모두 `null`이면 검증 생략). `birthDate`는 오늘보다 미래일 수 없다.
  - **[개정 — 리뷰 반영, `REVIEW-TASK-013-2026-08-21.md` Major #1]** "5개 필드 모두 포함"은 값이 아니라 **JSON 키 자체의 존재 여부**로 강제해야 한다. `UpdateStudentProfileRequest`가 필드 생략과 명시적 `null`을 구분하지 못하면, 클라이언트가 일부 필드만 보낸 실수를 부분 수정으로 착각해도 나머지 필드가 조용히 `null`로 덮어써진다(데이터 유실). Jackson `@JsonProperty(required = true)`(nullable 타입에는 "키 존재"만 강제하고 값의 `null`은 허용) 또는 동등한 검증으로 구현하고, 5개 중 하나라도 키 자체가 없으면 400을 반환한다.
- 기존 `rename(newName)`은 변경 없음.

### Student 상세 화면성 조회

- `student/application/StudentService.kt`에 `ClassroomRepository`, `AttendanceRepository`, `HomeworkRepository`를 추가로 주입하고, `getStudentDetail(id: Long)` 메서드를 추가한다:
  1. `studentId`로 `Student` 조회(없으면 `StudentNotFoundException`, 기존 404 처리 재사용).
  2. `ClassroomRepository.findAllByStudentId(studentId)`로 소속 반 목록 조회.
  3. `AttendanceRepository.findAllByStudentId(studentId)`로 출결 전체 조회 → 상태별 개수 집계 + `createdAt` 내림차순 최근 5건 추출.
  4. `HomeworkRepository.findAllByStudentId(studentId)`(기존 메서드 재사용)로 숙제 전체 조회 → 상태별 개수 집계 + `createdAt` 내림차순 최근 5건 추출.
  5. 위 결과를 하나의 응답으로 조합해 반환.
- 새 Aggregate 간 연관관계(JPA `@ManyToOne` 등)는 만들지 않는다 — 각 Repository를 id로 개별 조회해 Application Service에서 조합한다(`ADR-005`).

### Repository 인터페이스 확장

- `classroom/domain/ClassroomRepository.kt`에 `findAllByStudentId(studentId: Long): List<Classroom>` 추가. 인프라 구현은 기존 `ClassStudentJpaRepository`(반-학생 배정 조인 테이블)로 `classId` 목록을 얻은 뒤 `ClassroomJpaRepository`로 일괄 조회하는 방식(`ClassroomRepositoryAdapter.kt` 확장).
- `attendance/domain/AttendanceRepository.kt`에 `findAllByStudentId(studentId: Long): List<Attendance>` 추가(현재는 `findAllByLessonId`만 존재). `AttendanceJpaRepository`에 파생 쿼리 메서드 추가, `AttendanceRepositoryAdapter.kt`에 위임 구현.
- `homework/domain/HomeworkRepository.kt`는 변경 없음 — `findAllByStudentId`가 이미 존재(`TASK-007`).

### 데이터베이스

- `V7__add_student_profile_fields.sql`:

```sql
ALTER TABLE students
    ADD COLUMN phone VARCHAR(20) NULL,
    ADD COLUMN birth_date DATE NULL,
    ADD COLUMN school VARCHAR(100) NULL,
    ADD COLUMN grade VARCHAR(20) NULL,
    ADD COLUMN memo VARCHAR(1000) NULL;
```

`students` 테이블에 컬럼만 추가하며, 다른 테이블에는 영향이 없다.

### API

- `student/presentation/StudentController.kt`에 엔드포인트 2종 추가(아래 API Changes 참고).
- `student/presentation/StudentDtos.kt`에 `UpdateStudentProfileRequest`, `StudentDetailResponse` 등 신규 DTO 추가. 기존 `StudentResponse`에도 신규 프로필 필드를 추가한다(기존 `POST`/`GET`/`PATCH`/`deactivate` 응답 모두 신규 필드를 포함하게 됨 — 값이 없으면 `null`).

### 인가 [개정 — `ADR-013` 반영]

- `SecurityConfig.kt`의 `/api/v1/students/**` 규칙을 다음과 같이 바꾼다:
  - `authorize(HttpMethod.GET, "/api/v1/students/**", hasAnyRole("ADMIN", "TEACHER"))` — 변경 없음(`GET .../detail` 포함).
  - `authorize("/api/v1/students/**", hasRole("ADMIN"))` → `authorize("/api/v1/students/**", hasAnyRole("ADMIN", "TEACHER"))`로 변경 — `POST /api/v1/students`, `PATCH /api/v1/students/{id}`, `PATCH /api/v1/students/{id}/profile`, `POST /api/v1/students/{id}/deactivate` 모두에 선생님 접근을 허용한다(`ADR-013`).
- **이미 이전 버전(선생님=조회만) 기준으로 구현된 코드가 있다면**, 이 규칙 변경과 함께 다음도 함께 수정한다:
  - `StudentApiTests`에서 "선생님이 프로필 수정/등록/이름수정/비활성화 시도 → 403"을 검증하던 테스트를 "선생님이 호출 → 200(성공)"으로 뒤집는다.
  - 관리자 전용을 가정한 다른 문구·주석이 남아있지 않은지 확인한다.

## Out of Scope

- "담당 학생" 스코핑 — 반-선생님 배정 기준 자체가 프로젝트 전체에서 미정(`DOMAIN_MODEL.md` §6). 이번에도 전체 학생 대상으로 조회를 허용한다.
- 학생 검색/목록 조회 자체의 고도화(필터링, 페이지네이션 등) — 이번 작업은 프로필 필드와 상세 조회에만 집중한다. 기존 `GET /api/v1/students`, `GET /api/v1/students/search`는 변경하지 않는다.
- 전화번호 형식(정규식) 검증 — 길이 제한만 둔다.
- 프로필 필드 개별 partial update(일부 필드만 수정) — 위 Scope 참고, 5개 필드를 항상 함께 보낸다.
- 프로필 필드별 조회 권한 차등(예: 선생님에게는 `memo`를 숨기는 등) — `USER_ROLES.md`에 그런 구분이 없다.
- 학습 기록(Learning Record) 요약을 상세 조회에 포함하는 것 — 해당 도메인이 아직 구현되지 않았다.
- 첨부파일(학생 사진 등) — 요구사항 근거 없음.
- 출결/숙제 요약의 기간 필터(예: "이번 달만") — 이번에는 전체 기록 기준 집계 + 최근 5건 고정 조회만 구현한다.

## Functional Scenarios

1. 관리자가 학생의 프로필(연락처, 생년월일, 학교, 학년, 메모)을 한 번에 등록/수정하면 해당 값들이 저장된다.
2. 관리자가 일부 필드만 비워서(`null`) 프로필을 수정하면, 그 필드는 값 없음으로 저장된다(예: 메모만 입력하고 나머지는 `null`).
3. 관리자 또는 선생님이 학생 상세 조회를 호출하면, 학생 기본 정보(신규 프로필 필드 포함) + 소속 반 목록 + 출결 요약(상태별 개수, 최근 5건) + 숙제 요약(상태별 개수, 최근 5건)이 함께 반환된다.
4. 반에 배정되지 않았거나 출결/숙제 기록이 없는 학생을 상세 조회하면, 해당 항목은 빈 배열/0건으로 반환되고 오류가 발생하지 않는다.
5. 생년월일을 미래 날짜로 프로필을 수정하려 하면 오류를 반환한다.
6. 연락처/학교/학년/메모가 각각의 길이 제한을 초과하면 오류를 반환한다.
7. 선생님이 프로필 수정(`PATCH .../profile`)을 시도하면 관리자와 동일하게 성공한다(`ADR-013`).
8. 존재하지 않는 학생 id로 프로필 수정/상세 조회를 호출하면 404를 반환한다.
9. 학생/학부모 역할 또는 미인증 사용자가 두 엔드포인트 중 하나를 호출하면 403/401을 반환한다.

## Business Rules

- `phone`은 20자, `school`/`grade`는 각 100자/20자, `memo`는 1000자를 초과할 수 없다(모두 `null`이면 검증 생략).
- `birthDate`는 오늘보다 미래일 수 없다.
- 상세 조회의 "최근 N건"은 `createdAt` 내림차순 기준 5건이다 — 출결/숙제에 별도의 "발생 일자" 필드가 없어 레코드 생성 시각을 기준으로 삼는다(이번 작업에서 채택한 가정).
- 상태별 개수 집계는 해당 학생의 전체 출결/숙제 레코드를 기준으로 한다(기간 제한 없음).

## API Changes

- `PATCH /api/v1/students/{id}/profile` — 200, body:

```json
{
  "phone": "string | null",
  "birthDate": "YYYY-MM-DD | null",
  "school": "string | null",
  "grade": "string | null",
  "memo": "string | null"
}
```

5개 필드 모두 요청 본문에 포함해야 한다(각각 `null` 가능) → `StudentResponse` 반환.

- `GET /api/v1/students/{id}/detail` — 200 / 404, `StudentDetailResponse`:

```json
{
  "id": "number",
  "name": "string",
  "status": "ACTIVE | INACTIVE",
  "phone": "string | null",
  "birthDate": "YYYY-MM-DD | null",
  "school": "string | null",
  "grade": "string | null",
  "memo": "string | null",
  "classrooms": [{ "id": "number", "name": "string", "status": "string" }],
  "attendanceSummary": {
    "counts": { "PRESENT": "number", "LATE": "number", "ABSENT": "number", "EXCUSED": "number" },
    "recent": [{ "id": "number", "lessonId": "number", "status": "string" }]
  },
  "homeworkSummary": {
    "counts": { "ASSIGNED": "number", "SUBMITTED": "number", "GRADED": "number" },
    "recent": [{ "id": "number", "lessonId": "number | null", "title": "string", "status": "string" }]
  }
}
```

- 기존 `StudentResponse`(`POST`/`GET`/`PATCH`/`deactivate` 응답)에도 `phone`/`birthDate`/`school`/`grade`/`memo` 필드가 추가된다(값이 없으면 `null`) — 기존 클라이언트가 무시 가능한 하위호환 확장.

## Domain Impact

- `student/domain/Student.kt` — 필드 5개 추가, `updateProfile` 메서드 추가(위 Scope 참고). Aggregate 경계 자체는 변경 없음.
- `student/application/StudentService.kt` — `ClassroomRepository`/`AttendanceRepository`/`HomeworkRepository` 의존성 추가, `getStudentDetail` 추가. `Student`가 다른 Aggregate를 참조하는 구조로 바뀌는 것은 아니다 — 조회 시점에만 각 Repository를 개별 호출해 조합한다(`ADR-005`).
- `classroom/domain/ClassroomRepository.kt`, `attendance/domain/AttendanceRepository.kt` — 조회 메서드 각 1개 추가(위 Scope 참고).

## Database Impact

`V7__add_student_profile_fields.sql` — `students` 테이블에 컬럼 5개 추가(위 Scope 참고). 다른 테이블 스키마 변경 없음.

## Exception and Error Handling

- 존재하지 않는 학생 id로 프로필 수정/상세 조회 → 404(`StudentNotFoundException`, 기존 재사용).
- `phone`/`school`/`grade`/`memo` 길이 초과, `birthDate` 미래 날짜 → 400(도메인 `require` 검증에서 발생, Spring 기본 처리로 400 매핑).
- **[개정]** `PATCH .../profile` 요청 본문에 5개 필드 중 하나라도 키 자체가 없으면 → 400(요청이 거부되고, 기존 프로필 값은 변경되지 않는다).
- 학생/학부모 역할 또는 미인증 사용자가 두 엔드포인트 중 하나를 호출 → 403/401.

## Test Scenarios

- `StudentTest`(도메인 단위): `updateProfile` 정상 동작, 각 필드 길이 초과 시 예외, 미래 생년월일 시 예외, 전체 필드 `null`로 교체 가능.
- `StudentApiTests`(MockMvc + Testcontainers):
  - `PATCH /api/v1/students/{id}/profile` 정상 동작(관리자 **및 선생님** 각각) → `GET`으로 재조회 시 반영 확인.
  - 프로필 필드 길이 초과/미래 생년월일 → 400.
  - **[개정]** 5개 필드 중 하나(예: `phone`)를 아예 생략한 `PATCH .../profile` 요청 → 400을 반환하고, 그 이전에 저장돼 있던 프로필 값(다른 필드 포함)이 그대로 유지되는지 확인(`REVIEW-TASK-013-2026-08-21.md` Major #1 회귀 테스트).
  - 존재하지 않는 id로 프로필 수정/상세 조회 → 404.
  - `GET /api/v1/students/{id}/detail` — 반/출결/숙제가 있는 학생에 대해 소속 반 목록·출결 요약(개수+최근 5건)·숙제 요약(개수+최근 5건)이 정확히 반환되는지 확인.
  - 반/출결/숙제가 전혀 없는 학생의 상세 조회 — 빈 배열/0건으로 오류 없이 반환되는지 확인.
  - `POST /api/v1/students`, `PATCH /api/v1/students/{id}`, `POST /api/v1/students/{id}/deactivate`를 선생님이 호출해도 성공하는지 확인(`ADR-013`로 인한 회귀 방지 테스트 — 기존 TASK-002 테스트가 "선생님=거부"를 가정했다면 함께 수정).
  - ADMIN/TEACHER 외 역할(학생/학부모) 또는 미인증 사용자가 두 신규 엔드포인트와 기존 학생 관리 엔드포인트를 호출하면 403/401.

## Acceptance Criteria

- 위 API 2종이 명세대로 동작한다.
- 기존 `StudentResponse`를 사용하는 API(등록/조회/목록/이름수정/비활성화)가 신규 필드를 포함해도 회귀 없이 동작한다.
- `./gradlew build`(spotlessCheck 포함)와 `./gradlew test`가 통과한다.
- `ClassroomRepository`/`AttendanceRepository`에 추가된 `findAllByStudentId`가 다른 기존 기능에 영향을 주지 않는다(순수 추가).

## Definition of Done

- `docs/DEVELOPMENT.md` § Definition of Done 기준을 만족한다.
- 위 Test Scenarios가 모두 통과하고, `./gradlew test`가 통과한다.
- Implementation Agent의 self-check 이후, 별도의 독립적인 Review Agent 검토를 거친다(`AGENTS.md`, `CLAUDE.md` 원칙).

## Open Questions

- "담당 학생" 스코핑은 이번에도 해소되지 않는다 — 반-선생님 배정 기준이 확정되면 선생님의 조회 범위를 제한하는 별도 작업이 필요하다.
- 프로필 필드별 세부 조회/수정 권한 차등(예: 메모를 선생님에게 숨길지 여부)이 실제로 필요한지는 운영 중 재확인이 필요하다.
- 출결/숙제 요약의 "최근 5건" 기준(`createdAt`)이 실제 업무상 의미 있는 정렬인지(예: 수업 일자 기준이 더 적절할 수 있음)는 `Lesson`에 일자 필드가 도입되면 재검토가 필요하다.

## Related ADRs

- `docs/adr/ADR-002-tsid-as-identifier.md`
- `docs/adr/ADR-003-common-not-found-error-handling.md`
- `docs/adr/ADR-005-query-strategy.md`
- `docs/adr/ADR-007-attendance-homework-learningrecord-aggregate-boundary.md`
- `docs/adr/ADR-013-teacher-admin-parity-for-student-parent-domain.md`
