package com.umtle.umtleapi.attendance.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AttendanceTest {
    @Test
    fun `record creates attendance with lesson student and status`() {
        val attendance = Attendance.record(1L, 2L, AttendanceStatus.PRESENT)

        assertTrue(attendance.id > 0)
        assertEquals(1L, attendance.lessonId)
        assertEquals(2L, attendance.studentId)
        assertEquals(AttendanceStatus.PRESENT, attendance.status)
    }

    @Test
    fun `updateStatus changes attendance status`() {
        val attendance = Attendance.record(1L, 2L, AttendanceStatus.PRESENT)

        attendance.updateStatus(AttendanceStatus.LATE)

        assertEquals(AttendanceStatus.LATE, attendance.status)
    }
}
