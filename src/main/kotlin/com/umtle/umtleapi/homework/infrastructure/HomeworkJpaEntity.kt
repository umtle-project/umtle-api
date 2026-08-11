package com.umtle.umtleapi.homework.infrastructure

import com.umtle.umtleapi.common.infrastructure.BaseEntity
import com.umtle.umtleapi.homework.domain.HomeworkStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "homeworks")
class HomeworkJpaEntity(
    @Id
    val id: Long,
    @Column(name = "student_id", nullable = false)
    var studentId: Long,
    @Column(name = "lesson_id")
    var lessonId: Long?,
    @Column(nullable = false, length = 100)
    var title: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: HomeworkStatus,
) : BaseEntity()
