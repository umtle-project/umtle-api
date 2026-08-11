package com.umtle.umtleapi.homework.domain

interface HomeworkRepository {
    fun save(homework: Homework): Homework

    fun findById(id: Long): Homework?

    fun findAll(): List<Homework>

    fun findAllByStudentId(studentId: Long): List<Homework>

    fun deleteById(id: Long)
}
