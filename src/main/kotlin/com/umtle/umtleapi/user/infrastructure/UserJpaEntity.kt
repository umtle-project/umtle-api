package com.umtle.umtleapi.user.infrastructure

import com.umtle.umtleapi.common.infrastructure.BaseEntity
import com.umtle.umtleapi.user.domain.UserRole
import com.umtle.umtleapi.user.domain.UserStatus
import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import org.hibernate.annotations.BatchSize

@Entity
@Table(name = "users")
class UserJpaEntity(
    @Id
    val id: Long,
    @Column(name = "login_id", nullable = false, unique = true, length = 50)
    var loginId: String,
    @Column(name = "password_hash", nullable = false, length = 100)
    var passwordHash: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: UserStatus,
    @ElementCollection
    @CollectionTable(
        name = "user_roles",
        joinColumns = [JoinColumn(name = "user_id")],
    )
    @Column(name = "role", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    @BatchSize(size = 100)
    var roles: MutableSet<UserRole> = mutableSetOf(),
) : BaseEntity()
