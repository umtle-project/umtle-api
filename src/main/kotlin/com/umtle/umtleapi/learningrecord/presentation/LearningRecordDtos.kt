package com.umtle.umtleapi.learningrecord.presentation

import com.umtle.umtleapi.learningrecord.domain.LearningRecord
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class RecordLearningRecordRequest(
    @field:NotNull
    val studentId: Long,
    @field:NotBlank
    @field:Size(max = LearningRecord.MAX_TITLE_LENGTH)
    val title: String,
    @field:NotBlank
    @field:Size(max = LearningRecord.MAX_CONTENT_LENGTH)
    val content: String,
)

data class UpdateLearningRecordRequest(
    @field:NotBlank
    @field:Size(max = LearningRecord.MAX_TITLE_LENGTH)
    val title: String,
    @field:NotBlank
    @field:Size(max = LearningRecord.MAX_CONTENT_LENGTH)
    val content: String,
)

data class LearningRecordResponse(
    val id: Long,
    val studentId: Long,
    val title: String,
    val content: String,
) {
    companion object {
        fun from(learningRecord: LearningRecord) =
            LearningRecordResponse(
                id = learningRecord.id,
                studentId = learningRecord.studentId,
                title = learningRecord.title,
                content = learningRecord.content,
            )
    }
}
