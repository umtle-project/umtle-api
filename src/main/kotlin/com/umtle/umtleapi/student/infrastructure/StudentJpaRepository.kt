package com.umtle.umtleapi.student.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface StudentJpaRepository : JpaRepository<StudentJpaEntity, Long> {
    fun findTop20ByNameContainingOrderByNameAsc(name: String): List<StudentJpaEntity>
}
