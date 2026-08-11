package com.umtle.umtleapi.homework.domain

import com.github.f4b6a3.tsid.TsidCreator

class Homework private constructor(
    val id: Long,
    val studentId: Long,
    val lessonId: Long?,
    title: String,
    status: HomeworkStatus,
) {
    var title: String = title
        private set

    var status: HomeworkStatus = status
        private set

    fun updateTitle(newTitle: String) {
        validateTitle(newTitle)
        title = newTitle
    }

    fun updateStatus(newStatus: HomeworkStatus) {
        status = newStatus
    }

    companion object {
        fun assign(
            studentId: Long,
            lessonId: Long?,
            title: String,
        ): Homework {
            validateTitle(title)
            return Homework(
                id = TsidCreator.getTsid().toLong(),
                studentId = studentId,
                lessonId = lessonId,
                title = title,
                status = HomeworkStatus.ASSIGNED,
            )
        }

        fun reconstitute(
            id: Long,
            studentId: Long,
            lessonId: Long?,
            title: String,
            status: HomeworkStatus,
        ): Homework {
            validateTitle(title)
            return Homework(
                id = id,
                studentId = studentId,
                lessonId = lessonId,
                title = title,
                status = status,
            )
        }

        private fun validateTitle(title: String) {
            require(title.isNotBlank()) { "숙제 제목은 비어 있을 수 없습니다." }
            require(title.length <= MAX_TITLE_LENGTH) { "숙제 제목은 100자를 초과할 수 없습니다." }
        }

        const val MAX_TITLE_LENGTH = 100
    }
}
