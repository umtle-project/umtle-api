package com.umtle.umtleapi.student.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface StudentJpaRepository : JpaRepository<StudentJpaEntity, Long>
