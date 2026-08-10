package com.umtle.umtleapi.lesson.domain

import com.github.f4b6a3.tsid.TsidCreator

class Lesson private constructor(
    val id: Long,
    val classId: Long,
    status: LessonStatus,
) {
    var status: LessonStatus = status
        private set

    fun cancel() {
        if (status != LessonStatus.SCHEDULED) {
            throw InvalidLessonStateTransitionException("cancel", status)
        }
        status = LessonStatus.CANCELLED
    }

    fun complete() {
        if (status != LessonStatus.SCHEDULED) {
            throw InvalidLessonStateTransitionException("complete", status)
        }
        status = LessonStatus.COMPLETED
    }

    companion object {
        fun register(classId: Long): Lesson =
            Lesson(
                id = TsidCreator.getTsid().toLong(),
                classId = classId,
                status = LessonStatus.SCHEDULED,
            )

        fun reconstitute(
            id: Long,
            classId: Long,
            status: LessonStatus,
        ): Lesson =
            Lesson(
                id = id,
                classId = classId,
                status = status,
            )
    }
}
