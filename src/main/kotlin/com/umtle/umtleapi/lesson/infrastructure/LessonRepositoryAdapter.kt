package com.umtle.umtleapi.lesson.infrastructure

import com.umtle.umtleapi.lesson.domain.Lesson
import com.umtle.umtleapi.lesson.domain.LessonRepository
import org.springframework.stereotype.Repository

@Repository
class LessonRepositoryAdapter(
    private val jpaRepository: LessonJpaRepository,
) : LessonRepository {
    override fun save(lesson: Lesson): Lesson = jpaRepository.save(lesson.toEntity()).toDomain()

    override fun findById(id: Long): Lesson? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Lesson> = jpaRepository.findAll().map { it.toDomain() }

    private fun Lesson.toEntity() =
        LessonJpaEntity(
            id = id,
            classId = classId,
            status = status,
        )

    private fun LessonJpaEntity.toDomain() =
        Lesson.reconstitute(
            id = id,
            classId = classId,
            status = status,
        )
}
