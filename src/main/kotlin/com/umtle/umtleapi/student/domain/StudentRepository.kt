package com.umtle.umtleapi.student.domain

interface StudentRepository {
    fun save(student: Student): Student

    fun findById(id: Long): Student?

    fun existsById(id: Long): Boolean

    fun findAll(): List<Student>

    fun searchByName(name: String): List<Student>
}
