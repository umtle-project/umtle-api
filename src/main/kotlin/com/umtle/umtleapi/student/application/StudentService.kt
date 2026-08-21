package com.umtle.umtleapi.student.application

import com.umtle.umtleapi.attendance.domain.Attendance
import com.umtle.umtleapi.attendance.domain.AttendanceRepository
import com.umtle.umtleapi.attendance.domain.AttendanceStatus
import com.umtle.umtleapi.classroom.domain.Classroom
import com.umtle.umtleapi.classroom.domain.ClassroomRepository
import com.umtle.umtleapi.homework.domain.Homework
import com.umtle.umtleapi.homework.domain.HomeworkRepository
import com.umtle.umtleapi.homework.domain.HomeworkStatus
import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentNotFoundException
import com.umtle.umtleapi.student.domain.StudentRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Service
class StudentService(
    private val studentRepository: StudentRepository,
    private val classroomRepository: ClassroomRepository,
    private val attendanceRepository: AttendanceRepository,
    private val homeworkRepository: HomeworkRepository,
) {
    @Transactional
    fun registerStudent(name: String): Student = studentRepository.save(Student.register(name))

    @Transactional(readOnly = true)
    fun getStudent(id: Long): Student = studentRepository.findById(id) ?: throw StudentNotFoundException(id)

    @Transactional(readOnly = true)
    fun getStudentDetail(id: Long): StudentDetail {
        val student = getStudent(id)
        val attendances = attendanceRepository.findAllByStudentId(id)
        val homeworks = homeworkRepository.findAllByStudentId(id)

        return StudentDetail(
            student = student,
            classrooms = classroomRepository.findAllByStudentId(id),
            attendanceSummary =
                AttendanceSummary(
                    counts = AttendanceStatus.entries.associateWith { status -> attendances.count { it.status == status } },
                    recent = attendances.take(5),
                ),
            homeworkSummary =
                HomeworkSummary(
                    counts = HomeworkStatus.entries.associateWith { status -> homeworks.count { it.status == status } },
                    recent = homeworks.take(5),
                ),
        )
    }

    @Transactional(readOnly = true)
    fun listStudents(): List<Student> = studentRepository.findAll()

    @Transactional(readOnly = true)
    fun searchStudents(name: String): List<Student> {
        require(name.isNotBlank()) { "검색어는 비어 있을 수 없습니다." }
        return studentRepository.searchByName(name)
    }

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
    fun updateStudentProfile(
        id: Long,
        phone: String?,
        birthDate: LocalDate?,
        school: String?,
        grade: String?,
        memo: String?,
    ): Student {
        val student = getStudent(id)
        student.updateProfile(phone, birthDate, school, grade, memo)
        return studentRepository.save(student)
    }

    @Transactional
    fun deactivateStudent(id: Long): Student {
        val student = getStudent(id)
        student.deactivate()
        return studentRepository.save(student)
    }
}

data class StudentDetail(
    val student: Student,
    val classrooms: List<Classroom>,
    val attendanceSummary: AttendanceSummary,
    val homeworkSummary: HomeworkSummary,
)

data class AttendanceSummary(
    val counts: Map<AttendanceStatus, Int>,
    val recent: List<Attendance>,
)

data class HomeworkSummary(
    val counts: Map<HomeworkStatus, Int>,
    val recent: List<Homework>,
)
