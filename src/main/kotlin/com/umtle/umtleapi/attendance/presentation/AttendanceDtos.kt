package com.umtle.umtleapi.attendance.presentation

import com.umtle.umtleapi.attendance.domain.Attendance
import com.umtle.umtleapi.attendance.domain.AttendanceStatus
import jakarta.validation.constraints.NotNull

data class RecordAttendanceRequest(
    @field:NotNull
    val studentId: Long,
    @field:NotNull
    val status: AttendanceStatus,
)

data class UpdateAttendanceStatusRequest(
    @field:NotNull
    val status: AttendanceStatus,
)

data class AttendanceResponse(
    val id: Long,
    val lessonId: Long,
    val studentId: Long,
    val status: AttendanceStatus,
) {
    companion object {
        fun from(attendance: Attendance) =
            AttendanceResponse(
                id = attendance.id,
                lessonId = attendance.lessonId,
                studentId = attendance.studentId,
                status = attendance.status,
            )
    }
}
