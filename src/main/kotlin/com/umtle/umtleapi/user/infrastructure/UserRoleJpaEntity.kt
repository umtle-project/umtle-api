package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.user.domain.UserRole
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "user_roles")
class UserRoleJpaEntity(
    @Id
    val id: Long,
    @Column(name = "user_id", nullable = false)
    val userId: Long,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole,
)
