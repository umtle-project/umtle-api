package com.umtle.umtleapi.homework.infrastructure

import com.umtle.umtleapi.homework.domain.Homework
import com.umtle.umtleapi.homework.domain.HomeworkRepository
import org.springframework.stereotype.Repository

@Repository
class HomeworkRepositoryAdapter(
    private val jpaRepository: HomeworkJpaRepository,
) : HomeworkRepository {
    override fun save(homework: Homework): Homework = jpaRepository.save(homework.toEntity()).toDomain()

    override fun findById(id: Long): Homework? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Homework> = jpaRepository.findAll().map { it.toDomain() }

    override fun findAllByStudentId(studentId: Long): List<Homework> =
        jpaRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).map { it.toDomain() }

    override fun deleteById(id: Long) {
        jpaRepository.deleteById(id)
    }

    private fun Homework.toEntity() =
        HomeworkJpaEntity(
            id = id,
            studentId = studentId,
            lessonId = lessonId,
            title = title,
            status = status,
        )

    private fun HomeworkJpaEntity.toDomain() =
        Homework.reconstitute(
            id = id,
            studentId = studentId,
            lessonId = lessonId,
            title = title,
            status = status,
        )
}
