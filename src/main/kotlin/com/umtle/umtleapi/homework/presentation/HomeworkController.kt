package com.umtle.umtleapi.homework.presentation

import com.umtle.umtleapi.homework.application.HomeworkService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/homeworks")
class HomeworkController(
    private val homeworkService: HomeworkService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun assign(
        @Valid @RequestBody request: AssignHomeworkRequest,
    ): HomeworkResponse =
        HomeworkResponse.from(
            homeworkService.assign(
                studentId = request.studentId,
                lessonId = request.lessonId,
                title = request.title,
            ),
        )

    @GetMapping
    fun list(
        @RequestParam(required = false) studentId: Long?,
    ): List<HomeworkResponse> =
        if (studentId == null) {
            homeworkService.findAll()
        } else {
            homeworkService.findAllByStudentId(studentId)
        }.map { HomeworkResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): HomeworkResponse = HomeworkResponse.from(homeworkService.findById(id))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateHomeworkRequest,
    ): HomeworkResponse {
        request.validate()
        return HomeworkResponse.from(
            homeworkService.update(
                id = id,
                title = request.title,
                status = request.status,
            ),
        )
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun delete(
        @PathVariable id: Long,
    ) {
        homeworkService.delete(id)
    }
}
