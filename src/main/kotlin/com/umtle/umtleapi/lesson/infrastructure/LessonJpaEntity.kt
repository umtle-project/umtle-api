package com.umtle.umtleapi.lesson.infrastructure

import com.umtle.umtleapi.common.infrastructure.BaseEntity
import com.umtle.umtleapi.lesson.domain.LessonStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "lessons")
class LessonJpaEntity(
    @Id
    val id: Long,
    @Column(name = "class_id", nullable = false)
    var classId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: LessonStatus,
) : BaseEntity()
