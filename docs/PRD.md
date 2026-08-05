# Product Requirements Document

# 움틀 (Umtle)

> 학생의 가능성이 움트는 공간

---

## 1. 프로젝트 소개

움틀(Umtle)은 학생, 선생님, 학부모를 하나의 플랫폼으로 연결하여 학원의 운영과 학생의 학습 과정을 효율적으로 관리하는 웹 기반 서비스이다.

단순한 학원 관리 시스템을 넘어, 학생의 학습 기록과 성장 과정을 지속적으로 관리할 수 있는 플랫폼을 목표로 한다.

---

## 2. 목표

### Primary Goal

학원 운영에 필요한 핵심 기능을 하나의 서비스에서 제공한다.

### Long-term Goal

학생의 학습 데이터를 기반으로 성장 과정을 분석하고 시각화하는 플랫폼으로 발전한다.

---

## 3. 핵심 사용자

- 관리자
- 선생님
- 학생
- 학부모

각 사용자의 권한과 역할은 `USER_ROLES.md`에서 관리한다.

---

## 4. 핵심 기능

움틀은 다음 기능을 중심으로 개발한다.

- 학생 관리
- 반 및 수업 관리
- 일정 관리
- 출결 관리
- 숙제 관리
- 학습 기록 관리
- 공지 및 알림

세부 기능은 `REQUIREMENTS.md`에서 관리한다.

---

## 5. 기술 스택

### Backend

- Kotlin
- Spring Boot

### Frontend

- Next.js

### Database

- MySQL

### Infrastructure

- AWS

기술적인 설계는 `ARCHITECTURE.md`에서 관리한다.

---

## 6. 개발 원칙

- 단순한 구조를 우선한다.
- 도메인 중심으로 설계한다.
- 기능보다 유지보수성을 우선한다.
- 확장성을 고려하되 불필요한 추상화는 지양한다.
- AI와 협업하기 쉬운 구조와 문서를 유지한다.

---

## 7. 문서 관리

| 문서 | 목적 |
|------|------|
| PRD.md | 프로젝트 목표 및 범위 |
| REQUIREMENTS.md | 기능 명세 |
| DOMAIN_MODEL.md | 도메인 모델 |
| ARCHITECTURE.md | 시스템 설계 |
| ROADMAP.md | 개발 계획 |
| DECISIONS.md | 주요 설계 결정 |
| CLAUDE.md | Claude Code 개발 가이드 |
| AGENTS.md | AI 에이전트 공통 가이드 |