package com.umtle.umtleapi.classroom.presentation

import com.umtle.umtleapi.classroom.domain.Classroom
import com.umtle.umtleapi.classroom.domain.ClassroomStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RegisterClassroomRequest(
    @field:NotBlank
    @field:Size(max = Classroom.MAX_NAME_LENGTH)
    val name: String,
)

data class UpdateClassroomRequest(
    @field:NotBlank
    @field:Size(max = Classroom.MAX_NAME_LENGTH)
    val name: String,
)

data class AssignStudentRequest(
    @field:NotNull
    val studentId: Long,
)

data class AssignTeacherRequest(
    @field:NotNull
    val teacherId: Long,
)

data class ClassroomResponse(
    val id: Long,
    val name: String,
    val status: ClassroomStatus,
    val studentIds: Set<Long>,
    val teacherIds: Set<Long>,
) {
    companion object {
        fun from(classroom: Classroom) =
            ClassroomResponse(
                id = classroom.id,
                name = classroom.name,
                status = classroom.status,
                studentIds = classroom.studentIds,
                teacherIds = classroom.teacherIds,
            )
    }
}
