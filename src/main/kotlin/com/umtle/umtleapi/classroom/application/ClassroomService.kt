package com.umtle.umtleapi.classroom.application

import com.umtle.umtleapi.classroom.domain.Classroom
import com.umtle.umtleapi.classroom.domain.ClassroomNotFoundException
import com.umtle.umtleapi.classroom.domain.ClassroomRepository
import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import com.umtle.umtleapi.user.domain.UserNotFoundException
import com.umtle.umtleapi.user.domain.UserRepository
import com.umtle.umtleapi.user.domain.UserRole
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ClassroomService(
    private val classroomRepository: ClassroomRepository,
    private val studentRepository: StudentRepository,
    private val userRepository: UserRepository,
) {
    @Transactional
    fun registerClass(name: String): Classroom = classroomRepository.save(Classroom.register(name))

    @Transactional(readOnly = true)
    fun getClass(id: Long): Classroom = classroomRepository.findById(id) ?: throw ClassroomNotFoundException(id)

    @Transactional(readOnly = true)
    fun listClasses(): List<Classroom> = classroomRepository.findAll()

    @Transactional
    fun updateClass(
        id: Long,
        name: String,
    ): Classroom {
        val classroom = getClass(id)
        classroom.rename(name)
        return classroomRepository.save(classroom)
    }

    @Transactional
    fun deactivateClass(id: Long): Classroom {
        val classroom = getClass(id)
        classroom.deactivate()
        return classroomRepository.save(classroom)
    }

    @Transactional
    fun assignStudent(
        id: Long,
        studentId: Long,
    ): Classroom {
        val classroom = getClass(id)
        studentRepository.findById(studentId) ?: throw StudentNotFoundException(studentId)
        classroom.assignStudent(studentId)
        return classroomRepository.save(classroom)
    }

    @Transactional
    fun unassignStudent(
        id: Long,
        studentId: Long,
    ): Classroom {
        val classroom = getClass(id)
        classroom.unassignStudent(studentId)
        return classroomRepository.save(classroom)
    }

    @Transactional
    fun assignTeacher(
        id: Long,
        teacherId: Long,
    ): Classroom {
        val classroom = getClass(id)
        val teacher = userRepository.findById(teacherId) ?: throw UserNotFoundException(teacherId)
        if (UserRole.TEACHER !in teacher.roles) {
            throw InvalidTeacherAssignmentException(teacherId)
        }

        classroom.assignTeacher(teacherId)
        return classroomRepository.save(classroom)
    }

    @Transactional
    fun unassignTeacher(
        id: Long,
        teacherId: Long,
    ): Classroom {
        val classroom = getClass(id)
        classroom.unassignTeacher(teacherId)
        return classroomRepository.save(classroom)
    }
}
