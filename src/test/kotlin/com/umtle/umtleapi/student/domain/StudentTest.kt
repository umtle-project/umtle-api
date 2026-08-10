package com.umtle.umtleapi.student.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class StudentTest {
    @Test
    fun `register creates an active student with a positive id`() {
        val student = Student.register("홍길동")

        assertEquals("홍길동", student.name)
        assertEquals(StudentStatus.ACTIVE, student.status)
        assertTrue(student.id > 0)
    }

    @Test
    fun `register rejects a blank name`() {
        assertFailsWith<IllegalArgumentException> { Student.register("   ") }
    }

    @Test
    fun `rename rejects a blank name`() {
        val student = Student.register("홍길동")

        assertFailsWith<IllegalArgumentException> { student.rename(" ") }
    }

    @Test
    fun `register rejects a name longer than the persistence limit`() {
        assertFailsWith<IllegalArgumentException> { Student.register("가".repeat(Student.MAX_NAME_LENGTH + 1)) }
    }

    @Test
    fun `rename rejects a name longer than the persistence limit`() {
        val student = Student.register("홍길동")

        assertFailsWith<IllegalArgumentException> { student.rename("가".repeat(Student.MAX_NAME_LENGTH + 1)) }
    }

    @Test
    fun `deactivate transitions status to inactive`() {
        val student = Student.register("홍길동")

        student.deactivate()

        assertEquals(StudentStatus.INACTIVE, student.status)
    }

    @Test
    fun `deactivate is idempotent when already inactive`() {
        val student = Student.register("홍길동")
        student.deactivate()

        student.deactivate()

        assertEquals(StudentStatus.INACTIVE, student.status)
    }
}
