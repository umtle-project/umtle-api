package com.umtle.umtleapi.attendance.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class AttendanceNotFoundException(
    val attendanceId: Long,
) : AggregateNotFoundException("Attendance", attendanceId)
