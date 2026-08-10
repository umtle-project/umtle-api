package com.umtle.umtleapi.classroom.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.test.assertFailsWith

class ClassroomTest {
    @Test
    fun `register creates an active class`() {
        val classroom = Classroom.register("중등 수학")

        assertEquals("중등 수학", classroom.name)
        assertEquals(ClassroomStatus.ACTIVE, classroom.status)
        assertEquals(emptySet<Long>(), classroom.studentIds)
        assertEquals(emptySet<Long>(), classroom.teacherIds)
    }

    @Test
    fun `register rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> {
            Classroom.register("")
        }
    }

    @Test
    fun `student and teacher assignments are idempotent`() {
        val classroom = Classroom.register("중등 수학")

        classroom.assignStudent(1L)
        classroom.assignStudent(1L)
        classroom.assignTeacher(2L)
        classroom.assignTeacher(2L)

        assertEquals(setOf(1L), classroom.studentIds)
        assertEquals(setOf(2L), classroom.teacherIds)
    }

    @Test
    fun `unassign removes student and teacher ids`() {
        val classroom = Classroom.register("중등 수학")
        classroom.assignStudent(1L)
        classroom.assignTeacher(2L)

        classroom.unassignStudent(1L)
        classroom.unassignTeacher(2L)

        assertEquals(emptySet<Long>(), classroom.studentIds)
        assertEquals(emptySet<Long>(), classroom.teacherIds)
    }

    @Test
    fun `deactivate changes status to inactive`() {
        val classroom = Classroom.register("중등 수학")

        classroom.deactivate()

        assertEquals(ClassroomStatus.INACTIVE, classroom.status)
    }
}
