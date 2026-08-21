package com.umtle.umtleapi.learningrecord.presentation

import com.umtle.umtleapi.learningrecord.application.LearningRecordService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
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
@RequestMapping("/api/v1/learning-records")
class LearningRecordController(
    private val learningRecordService: LearningRecordService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun record(
        @Valid @RequestBody request: RecordLearningRecordRequest,
    ): LearningRecordResponse =
        LearningRecordResponse.from(
            learningRecordService.record(
                studentId = request.studentId,
                title = request.title,
                content = request.content,
            ),
        )

    @GetMapping
    fun list(
        @RequestParam studentId: Long,
    ): List<LearningRecordResponse> =
        learningRecordService
            .findAllByStudentId(studentId)
            .let { learningRecords ->
                learningRecords.mapIndexed { index, learningRecord ->
                    LearningRecordResponse.from(
                        learningRecord = learningRecord,
                        no = learningRecords.size - index,
                    )
                }
            }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): LearningRecordResponse = LearningRecordResponse.from(learningRecordService.findById(id))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateLearningRecordRequest,
    ): LearningRecordResponse =
        LearningRecordResponse.from(
            learningRecordService.update(
                id = id,
                title = request.title,
                content = request.content,
            ),
        )
}
