package com.umtle.umtleapi.lesson.presentation

import com.umtle.umtleapi.lesson.application.LessonService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/lessons")
class LessonController(
    private val lessonService: LessonService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterLessonRequest,
    ): LessonResponse = LessonResponse.from(lessonService.registerLesson(request.classId))

    @GetMapping
    fun list(): List<LessonResponse> = lessonService.listLessons().map { LessonResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): LessonResponse = LessonResponse.from(lessonService.getLesson(id))

    @PostMapping("/{id}/cancel")
    fun cancel(
        @PathVariable id: Long,
    ): LessonResponse = LessonResponse.from(lessonService.cancelLesson(id))

    @PostMapping("/{id}/complete")
    fun complete(
        @PathVariable id: Long,
    ): LessonResponse = LessonResponse.from(lessonService.completeLesson(id))
}
