package com.umtle.umtleapi.learningrecord.application

import com.umtle.umtleapi.learningrecord.domain.LearningRecord
import com.umtle.umtleapi.learningrecord.domain.LearningRecordNotFoundException
import com.umtle.umtleapi.learningrecord.domain.LearningRecordRepository
import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LearningRecordService(
    private val learningRecordRepository: LearningRecordRepository,
    private val studentRepository: StudentRepository,
) {
    @Transactional
    fun record(
        studentId: Long,
        title: String,
        content: String,
    ): LearningRecord {
        validateStudentExists(studentId)
        return learningRecordRepository.save(
            LearningRecord.record(
                studentId = studentId,
                title = title,
                content = content,
            ),
        )
    }

    @Transactional
    fun update(
        id: Long,
        title: String,
        content: String,
    ): LearningRecord {
        val learningRecord = findById(id)
        learningRecord.update(
            newTitle = title,
            newContent = content,
        )
        return learningRecordRepository.save(learningRecord)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): LearningRecord = learningRecordRepository.findById(id) ?: throw LearningRecordNotFoundException(id)

    @Transactional(readOnly = true)
    fun findAllByStudentId(studentId: Long): List<LearningRecord> {
        validateStudentExists(studentId)
        return learningRecordRepository.findAllByStudentId(studentId)
    }

    private fun validateStudentExists(studentId: Long) {
        if (!studentRepository.existsById(studentId)) {
            throw StudentNotFoundException(studentId)
        }
    }
}
