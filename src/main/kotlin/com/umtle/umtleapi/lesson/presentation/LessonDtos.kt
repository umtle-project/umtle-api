package com.umtle.umtleapi.lesson.presentation

import com.umtle.umtleapi.lesson.domain.Lesson
import com.umtle.umtleapi.lesson.domain.LessonStatus
import jakarta.validation.constraints.NotNull

data class RegisterLessonRequest(
    @field:NotNull
    val classId: Long,
)

data class LessonResponse(
    val id: Long,
    val classId: Long,
    val status: LessonStatus,
) {
    companion object {
        fun from(lesson: Lesson) =
            LessonResponse(
                id = lesson.id,
                classId = lesson.classId,
                status = lesson.status,
            )
    }
}
