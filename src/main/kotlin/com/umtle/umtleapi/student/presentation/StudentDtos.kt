package com.umtle.umtleapi.student.presentation

import com.fasterxml.jackson.annotation.JsonProperty
import com.umtle.umtleapi.attendance.domain.Attendance
import com.umtle.umtleapi.attendance.domain.AttendanceStatus
import com.umtle.umtleapi.classroom.domain.Classroom
import com.umtle.umtleapi.classroom.domain.ClassroomStatus
import com.umtle.umtleapi.homework.domain.Homework
import com.umtle.umtleapi.homework.domain.HomeworkStatus
import com.umtle.umtleapi.student.application.AttendanceSummary
import com.umtle.umtleapi.student.application.HomeworkSummary
import com.umtle.umtleapi.student.application.StudentDetail
import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PastOrPresent
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class RegisterStudentRequest(
    @field:NotBlank
    @field:Size(max = Student.MAX_NAME_LENGTH)
    val name: String,
)

data class UpdateStudentRequest(
    @field:NotBlank
    @field:Size(max = Student.MAX_NAME_LENGTH)
    val name: String,
)

data class UpdateStudentProfileRequest(
    @param:JsonProperty(required = true)
    @field:Size(max = Student.MAX_PHONE_LENGTH)
    val phone: String?,
    @param:JsonProperty(required = true)
    @field:PastOrPresent
    val birthDate: LocalDate?,
    @param:JsonProperty(required = true)
    @field:Size(max = Student.MAX_SCHOOL_LENGTH)
    val school: String?,
    @param:JsonProperty(required = true)
    @field:Size(max = Student.MAX_GRADE_LENGTH)
    val grade: String?,
    @param:JsonProperty(required = true)
    @field:Size(max = Student.MAX_MEMO_LENGTH)
    val memo: String?,
)

data class StudentResponse(
    val id: Long,
    val name: String,
    val status: StudentStatus,
    val phone: String?,
    val birthDate: LocalDate?,
    val school: String?,
    val grade: String?,
    val memo: String?,
) {
    companion object {
        fun from(student: Student) =
            StudentResponse(
                id = student.id,
                name = student.name,
                status = student.status,
                phone = student.phone,
                birthDate = student.birthDate,
                school = student.school,
                grade = student.grade,
                memo = student.memo,
            )
    }
}

data class StudentSearchResponse(
    val id: Long,
    val name: String,
) {
    companion object {
        fun from(student: Student) =
            StudentSearchResponse(
                id = student.id,
                name = student.name,
            )
    }
}

data class StudentDetailResponse(
    val id: Long,
    val name: String,
    val status: StudentStatus,
    val phone: String?,
    val birthDate: LocalDate?,
    val school: String?,
    val grade: String?,
    val memo: String?,
    val classrooms: List<StudentClassroomResponse>,
    val attendanceSummary: StudentAttendanceSummaryResponse,
    val homeworkSummary: StudentHomeworkSummaryResponse,
) {
    companion object {
        fun from(detail: StudentDetail): StudentDetailResponse {
            val student = detail.student
            return StudentDetailResponse(
                id = student.id,
                name = student.name,
                status = student.status,
                phone = student.phone,
                birthDate = student.birthDate,
                school = student.school,
                grade = student.grade,
                memo = student.memo,
                classrooms = detail.classrooms.map { StudentClassroomResponse.from(it) },
                attendanceSummary = StudentAttendanceSummaryResponse.from(detail.attendanceSummary),
                homeworkSummary = StudentHomeworkSummaryResponse.from(detail.homeworkSummary),
            )
        }
    }
}

data class StudentClassroomResponse(
    val id: Long,
    val name: String,
    val status: ClassroomStatus,
) {
    companion object {
        fun from(classroom: Classroom) =
            StudentClassroomResponse(
                id = classroom.id,
                name = classroom.name,
                status = classroom.status,
            )
    }
}

data class StudentAttendanceSummaryResponse(
    val counts: Map<AttendanceStatus, Int>,
    val recent: List<StudentAttendanceResponse>,
) {
    companion object {
        fun from(summary: AttendanceSummary) =
            StudentAttendanceSummaryResponse(
                counts = summary.counts,
                recent = summary.recent.map { StudentAttendanceResponse.from(it) },
            )
    }
}

data class StudentAttendanceResponse(
    val id: Long,
    val lessonId: Long,
    val status: AttendanceStatus,
) {
    companion object {
        fun from(attendance: Attendance) =
            StudentAttendanceResponse(
                id = attendance.id,
                lessonId = attendance.lessonId,
                status = attendance.status,
            )
    }
}

data class StudentHomeworkSummaryResponse(
    val counts: Map<HomeworkStatus, Int>,
    val recent: List<StudentHomeworkResponse>,
) {
    companion object {
        fun from(summary: HomeworkSummary) =
            StudentHomeworkSummaryResponse(
                counts = summary.counts,
                recent = summary.recent.map { StudentHomeworkResponse.from(it) },
            )
    }
}

data class StudentHomeworkResponse(
    val id: Long,
    val lessonId: Long?,
    val title: String,
    val status: HomeworkStatus,
) {
    companion object {
        fun from(homework: Homework) =
            StudentHomeworkResponse(
                id = homework.id,
                lessonId = homework.lessonId,
                title = homework.title,
                status = homework.status,
            )
    }
}
