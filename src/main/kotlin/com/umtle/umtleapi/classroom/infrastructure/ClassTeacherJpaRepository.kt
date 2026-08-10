package com.umtle.umtleapi.classroom.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface ClassTeacherJpaRepository : JpaRepository<ClassTeacherJpaEntity, Long> {
    fun findByClassId(classId: Long): List<ClassTeacherJpaEntity>
}
