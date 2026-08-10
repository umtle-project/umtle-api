package com.umtle.umtleapi.classroom.infrastructure

import com.github.f4b6a3.tsid.TsidCreator
import com.umtle.umtleapi.classroom.domain.Classroom
import com.umtle.umtleapi.classroom.domain.ClassroomRepository
import org.springframework.stereotype.Repository

@Repository
class ClassroomRepositoryAdapter(
    private val jpaRepository: ClassroomJpaRepository,
    private val studentJpaRepository: ClassStudentJpaRepository,
    private val teacherJpaRepository: ClassTeacherJpaRepository,
) : ClassroomRepository {
    override fun save(classroom: Classroom): Classroom {
        val savedClass = jpaRepository.save(classroom.toEntity())
        syncStudents(classroom.id, classroom.studentIds)
        syncTeachers(classroom.id, classroom.teacherIds)
        return savedClass.toDomain()
    }

    override fun findById(id: Long): Classroom? = jpaRepository.findById(id).orElse(null)?.toDomain()

    override fun findAll(): List<Classroom> = jpaRepository.findAll().map { it.toDomain() }

    private fun Classroom.toEntity() =
        ClassroomJpaEntity(
            id = id,
            name = name,
            status = status,
        )

    private fun ClassroomJpaEntity.toDomain() =
        Classroom.reconstitute(
            id = id,
            name = name,
            status = status,
            studentIds = studentJpaRepository.findByClassId(id).map { it.studentId }.toSet(),
            teacherIds = teacherJpaRepository.findByClassId(id).map { it.teacherId }.toSet(),
        )

    private fun Long.toStudentEntity(classId: Long) =
        ClassStudentJpaEntity(
            id = TsidCreator.getTsid().toLong(),
            classId = classId,
            studentId = this,
        )

    private fun Long.toTeacherEntity(classId: Long) =
        ClassTeacherJpaEntity(
            id = TsidCreator.getTsid().toLong(),
            classId = classId,
            teacherId = this,
        )

    private fun syncStudents(
        classId: Long,
        targetStudentIds: Set<Long>,
    ) {
        val currentStudents = studentJpaRepository.findByClassId(classId)
        val currentStudentIds = currentStudents.map { it.studentId }.toSet()

        studentJpaRepository.deleteAll(currentStudents.filter { it.studentId !in targetStudentIds })
        studentJpaRepository.saveAll((targetStudentIds - currentStudentIds).map { it.toStudentEntity(classId) })
    }

    private fun syncTeachers(
        classId: Long,
        targetTeacherIds: Set<Long>,
    ) {
        val currentTeachers = teacherJpaRepository.findByClassId(classId)
        val currentTeacherIds = currentTeachers.map { it.teacherId }.toSet()

        teacherJpaRepository.deleteAll(currentTeachers.filter { it.teacherId !in targetTeacherIds })
        teacherJpaRepository.saveAll((targetTeacherIds - currentTeacherIds).map { it.toTeacherEntity(classId) })
    }
}
