package com.umtle.umtleapi.student.domain

import java.time.LocalDate
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

    @Test
    fun `registerPending creates a pending student with a positive id`() {
        val student = Student.registerPending("홍길동")

        assertEquals("홍길동", student.name)
        assertEquals(StudentStatus.PENDING, student.status)
        assertTrue(student.id > 0)
    }

    @Test
    fun `activate transitions a pending student to active`() {
        val student = Student.registerPending("홍길동")

        student.activate()

        assertEquals(StudentStatus.ACTIVE, student.status)
    }

    @Test
    fun `activate rejects a student that is not pending`() {
        val student = Student.register("홍길동")

        assertFailsWith<IllegalArgumentException> { student.activate() }
    }

    @Test
    fun `updateProfile replaces profile fields`() {
        val student = Student.register("홍길동")
        val birthDate = LocalDate.now().minusYears(10)

        student.updateProfile(
            phone = "010-1234-5678",
            birthDate = birthDate,
            school = "움틀초",
            grade = "5학년",
            memo = "메모",
        )

        assertEquals("010-1234-5678", student.phone)
        assertEquals(birthDate, student.birthDate)
        assertEquals("움틀초", student.school)
        assertEquals("5학년", student.grade)
        assertEquals("메모", student.memo)
    }

    @Test
    fun `updateProfile can replace all profile fields with null`() {
        val student = Student.register("홍길동")
        student.updateProfile("010-1234-5678", LocalDate.now().minusYears(10), "움틀초", "5학년", "메모")

        student.updateProfile(
            phone = null,
            birthDate = null,
            school = null,
            grade = null,
            memo = null,
        )

        assertEquals(null, student.phone)
        assertEquals(null, student.birthDate)
        assertEquals(null, student.school)
        assertEquals(null, student.grade)
        assertEquals(null, student.memo)
    }

    @Test
    fun `updateProfile rejects values longer than profile limits`() {
        val student = Student.register("홍길동")

        assertFailsWith<IllegalArgumentException> {
            student.updateProfile(
                phone = "1".repeat(Student.MAX_PHONE_LENGTH + 1),
                birthDate = null,
                school = null,
                grade = null,
                memo = null,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            student.updateProfile(
                phone = null,
                birthDate = null,
                school = "가".repeat(Student.MAX_SCHOOL_LENGTH + 1),
                grade = null,
                memo = null,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            student.updateProfile(
                phone = null,
                birthDate = null,
                school = null,
                grade = "가".repeat(Student.MAX_GRADE_LENGTH + 1),
                memo = null,
            )
        }
        assertFailsWith<IllegalArgumentException> {
            student.updateProfile(
                phone = null,
                birthDate = null,
                school = null,
                grade = null,
                memo = "가".repeat(Student.MAX_MEMO_LENGTH + 1),
            )
        }
    }

    @Test
    fun `updateProfile rejects future birthDate`() {
        val student = Student.register("홍길동")

        assertFailsWith<IllegalArgumentException> {
            student.updateProfile(
                phone = null,
                birthDate = LocalDate.now().plusDays(1),
                school = null,
                grade = null,
                memo = null,
            )
        }
    }
}
