package com.umtle.umtleapi.student.application

import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class StudentService(
    private val studentRepository: StudentRepository,
) {
    @Transactional
    fun registerStudent(name: String): Student = studentRepository.save(Student.register(name))

    @Transactional(readOnly = true)
    fun getStudent(id: Long): Student = studentRepository.findById(id) ?: throw StudentNotFoundException(id)

    @Transactional(readOnly = true)
    fun listStudents(): List<Student> = studentRepository.findAll()

    @Transactional
    fun updateStudent(
        id: Long,
        name: String,
    ): Student {
        val student = getStudent(id)
        student.rename(name)
        return studentRepository.save(student)
    }

    @Transactional
    fun deactivateStudent(id: Long): Student {
        val student = getStudent(id)
        student.deactivate()
        return studentRepository.save(student)
    }
}
