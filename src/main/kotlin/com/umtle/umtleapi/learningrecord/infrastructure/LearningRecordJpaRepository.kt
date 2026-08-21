package com.umtle.umtleapi.learningrecord.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface LearningRecordJpaRepository : JpaRepository<LearningRecordJpaEntity, Long> {
    fun findAllByStudentIdOrderByCreatedAtDesc(studentId: Long): List<LearningRecordJpaEntity>
}
