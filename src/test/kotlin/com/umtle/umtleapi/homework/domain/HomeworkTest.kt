package com.umtle.umtleapi.homework.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class HomeworkTest {
    @Test
    fun `assign creates homework with assigned status`() {
        val homework = Homework.assign(studentId = 1L, lessonId = null, title = "문제집 10쪽")

        assertTrue(homework.id > 0)
        assertEquals(1L, homework.studentId)
        assertNull(homework.lessonId)
        assertEquals("문제집 10쪽", homework.title)
        assertEquals(HomeworkStatus.ASSIGNED, homework.status)
    }

    @Test
    fun `assign creates lesson based homework`() {
        val homework = Homework.assign(studentId = 1L, lessonId = 2L, title = "오답 정리")

        assertEquals(2L, homework.lessonId)
    }

    @Test
    fun `updateTitle changes title`() {
        val homework = Homework.assign(studentId = 1L, lessonId = null, title = "문제집 10쪽")

        homework.updateTitle("문제집 11쪽")

        assertEquals("문제집 11쪽", homework.title)
    }

    @Test
    fun `updateStatus changes status`() {
        val homework = Homework.assign(studentId = 1L, lessonId = null, title = "문제집 10쪽")

        homework.updateStatus(HomeworkStatus.SUBMITTED)

        assertEquals(HomeworkStatus.SUBMITTED, homework.status)
    }

    @Test
    fun `title cannot be blank`() {
        assertFailsWith<IllegalArgumentException> {
            Homework.assign(studentId = 1L, lessonId = null, title = " ")
        }
    }

    @Test
    fun `title cannot exceed persistence limit`() {
        assertFailsWith<IllegalArgumentException> {
            Homework.assign(studentId = 1L, lessonId = null, title = "가".repeat(Homework.MAX_TITLE_LENGTH + 1))
        }
    }
}
