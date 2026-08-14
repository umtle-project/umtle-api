package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.common.infrastructure.BaseEntity
import com.umtle.umtleapi.user.domain.UserStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    val id: Long,
    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    var loginId: String,
    @Column(nullable = false, length = 100)
    var name: String,
    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus,
    @Column(name = "student_id", unique = true)
    var studentId: Long?,
) : BaseEntity()
