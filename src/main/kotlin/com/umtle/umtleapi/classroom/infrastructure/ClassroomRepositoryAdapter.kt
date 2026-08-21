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
        jpaRepository.save(classroom.toEntity())
        syncStudents(classroom.id, classroom.studentIds)
        syncTeachers(classroom.id, classroom.teacherIds)
        return classroom
    }

    override fun findById(id: Long): Classroom? = jpaRepository.findById(id).orElse(null)?.loadDomain()

    override fun findAll(): List<Classroom> {
        val classes = jpaRepository.findAll()
        val classIds = classes.map { it.id }
        val studentIdsByClassId = findStudentIdsByClassId(classIds)
        val teacherIdsByClassId = findTeacherIdsByClassId(classIds)

        return classes.map {
            it.toDomain(
                studentIds = studentIdsByClassId[it.id].orEmpty(),
                teacherIds = teacherIdsByClassId[it.id].orEmpty(),
            )
        }
    }

    override fun findAllByStudentId(studentId: Long): List<Classroom> {
        val classIds = studentJpaRepository.findByStudentId(studentId).map { it.classId }
        if (classIds.isEmpty()) {
            return emptyList()
        }
        val classes = jpaRepository.findAllById(classIds)
        val studentIdsByClassId = findStudentIdsByClassId(classIds)
        val teacherIdsByClassId = findTeacherIdsByClassId(classIds)

        return classes.map {
            it.toDomain(
                studentIds = studentIdsByClassId[it.id].orEmpty(),
                teacherIds = teacherIdsByClassId[it.id].orEmpty(),
            )
        }
    }

    override fun existsById(id: Long): Boolean = jpaRepository.existsById(id)

    override fun existsStudentAssignment(
        classId: Long,
        studentId: Long,
    ): Boolean = studentJpaRepository.existsByClassIdAndStudentId(classId, studentId)

    private fun findStudentIdsByClassId(classIds: List<Long>): Map<Long, Set<Long>> {
        if (classIds.isEmpty()) {
            return emptyMap()
        }
        return studentJpaRepository
            .findByClassIdIn(classIds)
            .groupBy { it.classId }
            .mapValues { (_, students) -> students.map { it.studentId }.toSet() }
    }

    private fun findTeacherIdsByClassId(classIds: List<Long>): Map<Long, Set<Long>> {
        if (classIds.isEmpty()) {
            return emptyMap()
        }
        return teacherJpaRepository
            .findByClassIdIn(classIds)
            .groupBy { it.classId }
            .mapValues { (_, teachers) -> teachers.map { it.teacherId }.toSet() }
    }

    private fun Classroom.toEntity() =
        ClassroomJpaEntity(
            id = id,
            name = name,
            status = status,
        )

    private fun ClassroomJpaEntity.loadDomain() =
        toDomain(
            studentIds = studentJpaRepository.findByClassId(id).map { it.studentId }.toSet(),
            teacherIds = teacherJpaRepository.findByClassId(id).map { it.teacherId }.toSet(),
        )

    private fun ClassroomJpaEntity.toDomain(
        studentIds: Set<Long>,
        teacherIds: Set<Long>,
    ) = Classroom.reconstitute(
        id = id,
        name = name,
        status = status,
        studentIds = studentIds,
        teacherIds = teacherIds,
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
