package com.umtle.umtleapi.lesson.application

import com.umtle.umtleapi.classroom.domain.ClassroomNotFoundException
import com.umtle.umtleapi.classroom.domain.ClassroomRepository
import com.umtle.umtleapi.lesson.domain.Lesson
import com.umtle.umtleapi.lesson.domain.LessonNotFoundException
import com.umtle.umtleapi.lesson.domain.LessonRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class LessonService(
    private val lessonRepository: LessonRepository,
    private val classroomRepository: ClassroomRepository,
) {
    @Transactional
    fun registerLesson(classId: Long): Lesson {
        if (!classroomRepository.existsById(classId)) {
            throw ClassroomNotFoundException(classId)
        }
        return lessonRepository.save(Lesson.register(classId))
    }

    @Transactional(readOnly = true)
    fun getLesson(id: Long): Lesson = lessonRepository.findById(id) ?: throw LessonNotFoundException(id)

    @Transactional(readOnly = true)
    fun listLessons(): List<Lesson> = lessonRepository.findAll()

    @Transactional
    fun cancelLesson(id: Long): Lesson {
        val lesson = getLesson(id)
        lesson.cancel()
        return lessonRepository.save(lesson)
    }

    @Transactional
    fun completeLesson(id: Long): Lesson {
        val lesson = getLesson(id)
        lesson.complete()
        return lessonRepository.save(lesson)
    }
}
