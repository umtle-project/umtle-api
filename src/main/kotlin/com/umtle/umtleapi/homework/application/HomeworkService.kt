package com.umtle.umtleapi.homework.application

import com.umtle.umtleapi.classroom.domain.ClassroomRepository
import com.umtle.umtleapi.homework.domain.Homework
import com.umtle.umtleapi.homework.domain.HomeworkNotFoundException
import com.umtle.umtleapi.homework.domain.HomeworkRepository
import com.umtle.umtleapi.homework.domain.HomeworkStatus
import com.umtle.umtleapi.lesson.domain.LessonNotFoundException
import com.umtle.umtleapi.lesson.domain.LessonRepository
import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class HomeworkService(
    private val homeworkRepository: HomeworkRepository,
    private val studentRepository: StudentRepository,
    private val lessonRepository: LessonRepository,
    private val classroomRepository: ClassroomRepository,
) {
    @Transactional
    fun assign(
        studentId: Long,
        lessonId: Long?,
        title: String,
    ): Homework {
        if (!studentRepository.existsById(studentId)) {
            throw StudentNotFoundException(studentId)
        }
        validateLessonAssignment(studentId, lessonId)
        return homeworkRepository.save(
            Homework.assign(
                studentId = studentId,
                lessonId = lessonId,
                title = title,
            ),
        )
    }

    @Transactional
    fun update(
        id: Long,
        title: String?,
        status: HomeworkStatus?,
    ): Homework {
        if (title == null && status == null) {
            throw InvalidHomeworkUpdateException("Either title or status must be provided")
        }

        val homework = findById(id)
        title?.let { homework.updateTitle(it) }
        status?.let { homework.updateStatus(it) }
        return homeworkRepository.save(homework)
    }

    @Transactional
    fun delete(id: Long) {
        findById(id)
        homeworkRepository.deleteById(id)
    }

    @Transactional(readOnly = true)
    fun findById(id: Long): Homework = homeworkRepository.findById(id) ?: throw HomeworkNotFoundException(id)

    @Transactional(readOnly = true)
    fun findAll(): List<Homework> = homeworkRepository.findAll()

    @Transactional(readOnly = true)
    fun findAllByStudentId(studentId: Long): List<Homework> {
        if (!studentRepository.existsById(studentId)) {
            throw StudentNotFoundException(studentId)
        }
        return homeworkRepository.findAllByStudentId(studentId)
    }

    private fun validateLessonAssignment(
        studentId: Long,
        lessonId: Long?,
    ) {
        if (lessonId == null) {
            return
        }

        val lesson =
            lessonRepository.findById(lessonId) ?: throw LessonNotFoundException(lessonId)
        if (!classroomRepository.existsStudentAssignment(lesson.classId, studentId)) {
            throw UnassignedHomeworkStudentException(lessonId, studentId)
        }
    }
}
