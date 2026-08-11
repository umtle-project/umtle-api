package com.umtle.umtleapi.attendance.domain

import com.github.f4b6a3.tsid.TsidCreator

class Attendance private constructor(
    val id: Long,
    val lessonId: Long,
    val studentId: Long,
    status: AttendanceStatus,
) {
    var status: AttendanceStatus = status
        private set

    fun updateStatus(newStatus: AttendanceStatus) {
        status = newStatus
    }

    companion object {
        fun record(
            lessonId: Long,
            studentId: Long,
            status: AttendanceStatus,
        ): Attendance =
            Attendance(
                id = TsidCreator.getTsid().toLong(),
                lessonId = lessonId,
                studentId = studentId,
                status = status,
            )

        fun reconstitute(
            id: Long,
            lessonId: Long,
            studentId: Long,
            status: AttendanceStatus,
        ): Attendance =
            Attendance(
                id = id,
                lessonId = lessonId,
                studentId = studentId,
                status = status,
            )
    }
}
