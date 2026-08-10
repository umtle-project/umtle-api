package com.umtle.umtleapi.student.domain

interface StudentRepository {
    fun save(student: Student): Student

    fun findById(id: Long): Student?

    fun findAll(): List<Student>
}
