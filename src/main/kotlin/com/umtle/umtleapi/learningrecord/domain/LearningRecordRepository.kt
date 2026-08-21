package com.umtle.umtleapi.learningrecord.domain

interface LearningRecordRepository {
    fun save(learningRecord: LearningRecord): LearningRecord

    fun findById(id: Long): LearningRecord?

    fun findAllByStudentId(studentId: Long): List<LearningRecord>
}
