package com.umtle.umtleapi.classroom.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "class_students")
class ClassStudentJpaEntity(
    @Id
    val id: Long,
    @Column(name = "class_id", nullable = false)
    val classId: Long,
    @Column(name = "student_id", nullable = false)
    val studentId: Long,
)
