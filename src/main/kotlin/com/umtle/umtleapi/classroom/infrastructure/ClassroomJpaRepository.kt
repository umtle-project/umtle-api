package com.umtle.umtleapi.classroom.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface ClassroomJpaRepository : JpaRepository<ClassroomJpaEntity, Long>
