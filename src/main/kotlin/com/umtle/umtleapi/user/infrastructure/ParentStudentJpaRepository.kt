package com.umtle.umtleapi.user.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface ParentStudentJpaRepository : JpaRepository<ParentStudentJpaEntity, Long> {
    fun findByParentUserId(parentUserId: Long): List<ParentStudentJpaEntity>
}
