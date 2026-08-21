package com.umtle.umtleapi.homework.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface HomeworkJpaRepository : JpaRepository<HomeworkJpaEntity, Long> {
    fun findAllByStudentIdOrderByCreatedAtDesc(studentId: Long): List<HomeworkJpaEntity>
}
