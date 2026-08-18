package com.umtle.umtleapi.classroom.domain

interface ClassroomRepository {
    fun save(classroom: Classroom): Classroom

    fun findById(id: Long): Classroom?

    fun findAll(): List<Classroom>

    fun existsById(id: Long): Boolean

    fun existsStudentAssignment(
        classId: Long,
        studentId: Long,
    ): Boolean
}
