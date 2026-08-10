package com.umtle.umtleapi.classroom.infrastructure

import com.umtle.umtleapi.classroom.domain.ClassroomStatus
import com.umtle.umtleapi.common.infrastructure.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table

@Entity
@Table(name = "classes")
class ClassroomJpaEntity(
    @Id
    val id: Long,
    @Column(nullable = false, length = 100)
    var name: String,
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ClassroomStatus,
) : BaseEntity()
