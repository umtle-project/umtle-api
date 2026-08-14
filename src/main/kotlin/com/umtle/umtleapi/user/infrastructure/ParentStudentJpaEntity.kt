package com.umtle.umtleapi.user.infrastructure

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "parent_student_links")
class ParentStudentJpaEntity(
    @Id
    val id: Long,
    @Column(name = "parent_user_id", nullable = false)
    val parentUserId: Long,
    @Column(name = "student_id", nullable = false)
    val studentId: Long,
)
