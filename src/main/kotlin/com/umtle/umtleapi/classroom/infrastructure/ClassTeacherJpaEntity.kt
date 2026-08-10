package com.umtle.umtleapi.classroom.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "class_teachers")
class ClassTeacherJpaEntity(
    @Id
    val id: Long,
    @Column(name = "class_id", nullable = false)
    val classId: Long,
    @Column(name = "teacher_id", nullable = false)
    val teacherId: Long,
)
