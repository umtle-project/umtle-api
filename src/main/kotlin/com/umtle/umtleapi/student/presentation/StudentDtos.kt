package com.umtle.umtleapi.student.presentation

import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

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

data class StudentResponse(
    val id: Long,
    val name: String,
    val status: StudentStatus,
) {
    companion object {
        fun from(student: Student) =
            StudentResponse(
                id = student.id,
                name = student.name,
                status = student.status,
            )
    }
}
