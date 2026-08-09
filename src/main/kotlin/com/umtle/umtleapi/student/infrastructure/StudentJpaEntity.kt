package com.umtle.umtleapi.student.infrastructure

import com.umtle.umtleapi.common.infrastructure.BaseEntity
import com.umtle.umtleapi.student.domain.StudentStatus
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

// id는 domain factory(Student.register)가 이미 TSID로 할당한 값을 그대로 저장한다 — 여기에 @GeneratedValue가 없는 것은 의도된 것이다(ADR-002).
@Entity
@Table(name = "students")
class StudentJpaEntity(
    @Id
    val id: Long,
    var name: String,
    @Enumerated(EnumType.STRING)
    var status: StudentStatus,
) : BaseEntity()
