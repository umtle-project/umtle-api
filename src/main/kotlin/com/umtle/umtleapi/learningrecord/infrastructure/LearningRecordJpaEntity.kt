package com.umtle.umtleapi.learningrecord.infrastructure

import com.umtle.umtleapi.common.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "learning_records")
class LearningRecordJpaEntity(
    @Id
    val id: Long,
    @Column(name = "student_id", nullable = false)
    var studentId: Long,
    @Column(nullable = false, length = 100)
    var title: String,
    @Column(nullable = false, length = 2000)
    var content: String,
) : BaseEntity()
