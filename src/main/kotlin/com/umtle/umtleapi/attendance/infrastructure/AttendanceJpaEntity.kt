package com.umtle.umtleapi.attendance.infrastructure

import com.umtle.umtleapi.attendance.domain.AttendanceStatus
import com.umtle.umtleapi.common.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "attendances")
class AttendanceJpaEntity(
    @Id
    val id: Long,
    @Column(name = "lesson_id", nullable = false)
    var lessonId: Long,
    @Column(name = "student_id", nullable = false)
    var studentId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: AttendanceStatus,
) : BaseEntity()
