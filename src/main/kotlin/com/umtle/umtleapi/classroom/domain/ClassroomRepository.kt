package com.umtle.umtleapi.classroom.domain

interface ClassroomRepository {
    fun save(classroom: Classroom): Classroom

    fun findById(id: Long): Classroom?

    fun findAll(): List<Classroom>
}
