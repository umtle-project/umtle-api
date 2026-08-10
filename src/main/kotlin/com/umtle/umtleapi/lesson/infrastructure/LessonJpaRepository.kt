package com.umtle.umtleapi.lesson.infrastructure

import org.springframework.data.jpa.repository.JpaRepository

interface LessonJpaRepository : JpaRepository<LessonJpaEntity, Long>
