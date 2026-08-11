package com.umtle.umtleapi.attendance.presentation

import com.umtle.umtleapi.attendance.application.AttendanceService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
class AttendanceController(
    private val attendanceService: AttendanceService,
) {
    @PostMapping("/api/v1/lessons/{lessonId}/attendances")
    @ResponseStatus(HttpStatus.CREATED)
    fun record(
        @PathVariable lessonId: Long,
        @Valid @RequestBody request: RecordAttendanceRequest,
    ): AttendanceResponse =
        AttendanceResponse.from(
            attendanceService.record(
                lessonId = lessonId,
                studentId = request.studentId,
                status = request.status,
            ),
        )

    @GetMapping("/api/v1/lessons/{lessonId}/attendances")
    fun listByLesson(
        @PathVariable lessonId: Long,
    ): List<AttendanceResponse> = attendanceService.findAllByLessonId(lessonId).map { AttendanceResponse.from(it) }

    @GetMapping("/api/v1/attendances")
    fun list(): List<AttendanceResponse> = attendanceService.findAll().map { AttendanceResponse.from(it) }

    @GetMapping("/api/v1/attendances/{id}")
    fun get(
        @PathVariable id: Long,
    ): AttendanceResponse = AttendanceResponse.from(attendanceService.findById(id))

    @PatchMapping("/api/v1/attendances/{id}")
    fun updateStatus(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateAttendanceStatusRequest,
    ): AttendanceResponse = AttendanceResponse.from(attendanceService.updateStatus(id, request.status))
}
