package com.umtle.umtleapi.lesson.domain

interface LessonRepository {
    fun save(lesson: Lesson): Lesson

    fun findById(id: Long): Lesson?

    fun findAll(): List<Lesson>
}
