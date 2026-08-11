package com.umtle.umtleapi.homework.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface HomeworkJpaRepository : JpaRepository<HomeworkJpaEntity, Long> {
    fun findAllByStudentId(studentId: Long): List<HomeworkJpaEntity>
}
