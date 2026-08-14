package com.umtle.umtleapi.student.infrastructure

import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentRepository
import org.springframework.stereotype.Repository

@Repository
class StudentRepositoryAdapter(
    private val jpaRepository: StudentJpaRepository,
) : StudentRepository {
    override fun save(student: Student): Student = jpaRepository.save(student.toEntity()).toDomain()

    override fun findById(id: Long): Student? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Student> = jpaRepository.findAll().map { it.toDomain() }

    override fun searchByName(name: String): List<Student> =
        jpaRepository.findTop20ByNameContainingOrderByNameAsc(name).map { it.toDomain() }

    private fun Student.toEntity() =
        StudentJpaEntity(
            id = id,
            name = name,
            status = status,
        )

    private fun StudentJpaEntity.toDomain() =
        Student.reconstitute(
            id = id,
            name = name,
            status = status,
        )
}
