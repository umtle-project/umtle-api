package com.umtle.umtleapi.homework.presentation

import com.umtle.umtleapi.homework.domain.Homework
import com.umtle.umtleapi.homework.domain.HomeworkStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AssignHomeworkRequest(
    @field:NotNull
    val studentId: Long,
    val lessonId: Long? = null,
    @field:NotBlank
    @field:Size(max = Homework.MAX_TITLE_LENGTH)
    val title: String,
)

data class UpdateHomeworkRequest(
    @field:Size(max = Homework.MAX_TITLE_LENGTH)
    val title: String? = null,
    val status: HomeworkStatus? = null,
) {
    fun validate() {
        if (title == null && status == null) {
            throw InvalidHomeworkRequestException("Either title or status must be provided")
        }
        if (title != null && title.isBlank()) {
            throw InvalidHomeworkRequestException("Homework title must not be blank")
        }
    }
}

data class HomeworkResponse(
    val id: Long,
    val studentId: Long,
    val lessonId: Long?,
    val title: String,
    val status: HomeworkStatus,
) {
    companion object {
        fun from(homework: Homework) =
            HomeworkResponse(
                id = homework.id,
                studentId = homework.studentId,
                lessonId = homework.lessonId,
                title = homework.title,
                status = homework.status,
            )
    }
}
