package com.umtle.umtleapi.learningrecord.domain

import com.github.f4b6a3.tsid.TsidCreator

class LearningRecord private constructor(
    val id: Long,
    val studentId: Long,
    title: String,
    content: String,
) {
    var title: String = title
        private set

    var content: String = content
        private set

    fun update(
        newTitle: String,
        newContent: String,
    ) {
        validateTitle(newTitle)
        validateContent(newContent)
        title = newTitle
        content = newContent
    }

    companion object {
        fun record(
            studentId: Long,
            title: String,
            content: String,
        ): LearningRecord {
            validateTitle(title)
            validateContent(content)
            return LearningRecord(
                id = TsidCreator.getTsid().toLong(),
                studentId = studentId,
                title = title,
                content = content,
            )
        }

        fun reconstitute(
            id: Long,
            studentId: Long,
            title: String,
            content: String,
        ): LearningRecord {
            validateTitle(title)
            validateContent(content)
            return LearningRecord(
                id = id,
                studentId = studentId,
                title = title,
                content = content,
            )
        }

        private fun validateTitle(title: String) {
            require(title.isNotBlank()) { "학습 기록 제목은 비어 있을 수 없습니다." }
            require(title.length <= MAX_TITLE_LENGTH) { "학습 기록 제목은 100자를 초과할 수 없습니다." }
        }

        private fun validateContent(content: String) {
            require(content.isNotBlank()) { "학습 기록 내용은 비어 있을 수 없습니다." }
            require(content.length <= MAX_CONTENT_LENGTH) { "학습 기록 내용은 2000자를 초과할 수 없습니다." }
        }

        const val MAX_TITLE_LENGTH = 100
        const val MAX_CONTENT_LENGTH = 2000
    }
}
