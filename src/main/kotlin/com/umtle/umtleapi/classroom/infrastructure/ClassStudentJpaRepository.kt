package com.umtle.umtleapi.classroom.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface ClassStudentJpaRepository : JpaRepository<ClassStudentJpaEntity, Long> {
    fun findByClassId(classId: Long): List<ClassStudentJpaEntity>
}
