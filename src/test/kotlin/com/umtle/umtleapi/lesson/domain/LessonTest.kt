package com.umtle.umtleapi.lesson.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class LessonTest {
    @Test
    fun `register creates a scheduled lesson`() {
        val lesson = Lesson.register(1L)

        assertEquals(1L, lesson.classId)
        assertEquals(LessonStatus.SCHEDULED, lesson.status)
    }

    @Test
    fun `cancel changes scheduled lesson to cancelled`() {
        val lesson = Lesson.register(1L)

        lesson.cancel()

        assertEquals(LessonStatus.CANCELLED, lesson.status)
    }

    @Test
    fun `complete changes scheduled lesson to completed`() {
        val lesson = Lesson.register(1L)

        lesson.complete()

        assertEquals(LessonStatus.COMPLETED, lesson.status)
    }

    @Test
    fun `completed lesson cannot be cancelled`() {
        val lesson = Lesson.register(1L)
        lesson.complete()

        assertFailsWith<InvalidLessonStateTransitionException> {
            lesson.cancel()
        }
    }

    @Test
    fun `cancelled lesson cannot be completed`() {
        val lesson = Lesson.register(1L)
        lesson.cancel()

        assertFailsWith<InvalidLessonStateTransitionException> {
            lesson.complete()
        }
    }
}
