package com.umtle.umtleapi.learningrecord.infrastructure

import com.umtle.umtleapi.learningrecord.domain.LearningRecord
import com.umtle.umtleapi.learningrecord.domain.LearningRecordRepository
import org.springframework.stereotype.Repository

@Repository
class LearningRecordRepositoryAdapter(
    private val jpaRepository: LearningRecordJpaRepository,
) : LearningRecordRepository {
    override fun save(learningRecord: LearningRecord): LearningRecord = jpaRepository.save(learningRecord.toEntity()).toDomain()

    override fun findById(id: Long): LearningRecord? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAllByStudentId(studentId: Long): List<LearningRecord> =
        jpaRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId).map { it.toDomain() }

    private fun LearningRecord.toEntity() =
        LearningRecordJpaEntity(
            id = id,
            studentId = studentId,
            title = title,
            content = content,
        )

    private fun LearningRecordJpaEntity.toDomain() =
        LearningRecord.reconstitute(
            id = id,
            studentId = studentId,
            createdAt = createdAt,
            title = title,
            content = content,
        )
}
