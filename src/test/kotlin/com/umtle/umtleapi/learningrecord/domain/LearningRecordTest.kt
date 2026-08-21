package com.umtle.umtleapi.learningrecord.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class LearningRecordTest {
    @Test
    fun `record creates learning record`() {
        val learningRecord = LearningRecord.record(studentId = 1L, title = "분수 복습", content = "약분 개념을 반복 연습함")

        assertTrue(learningRecord.id > 0)
        assertEquals(1L, learningRecord.studentId)
        assertEquals("분수 복습", learningRecord.title)
        assertEquals("약분 개념을 반복 연습함", learningRecord.content)
    }

    @Test
    fun `update changes title and content together`() {
        val learningRecord = LearningRecord.record(studentId = 1L, title = "분수 복습", content = "약분 개념을 반복 연습함")

        learningRecord.update(newTitle = "소수 복습", newContent = "소수점 이동을 연습함")

        assertEquals("소수 복습", learningRecord.title)
        assertEquals("소수점 이동을 연습함", learningRecord.content)
    }

    @Test
    fun `title cannot be blank`() {
        assertFailsWith<IllegalArgumentException> {
            LearningRecord.record(studentId = 1L, title = " ", content = "내용")
        }
    }

    @Test
    fun `title cannot exceed persistence limit`() {
        assertFailsWith<IllegalArgumentException> {
            LearningRecord.record(studentId = 1L, title = "가".repeat(LearningRecord.MAX_TITLE_LENGTH + 1), content = "내용")
        }
    }

    @Test
    fun `content cannot be blank`() {
        assertFailsWith<IllegalArgumentException> {
            LearningRecord.record(studentId = 1L, title = "제목", content = " ")
        }
    }

    @Test
    fun `content cannot exceed persistence limit`() {
        assertFailsWith<IllegalArgumentException> {
            LearningRecord.record(studentId = 1L, title = "제목", content = "가".repeat(LearningRecord.MAX_CONTENT_LENGTH + 1))
        }
    }
}
