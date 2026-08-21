package com.umtle.umtleapi.student.domain

import com.github.f4b6a3.tsid.TsidCreator
import java.time.LocalDate

class Student private constructor(
    val id: Long,
    name: String,
    status: StudentStatus,
    phone: String?,
    birthDate: LocalDate?,
    school: String?,
    grade: String?,
    memo: String?,
) {
    var name: String = name
        private set

    var status: StudentStatus = status
        private set

    var phone: String? = phone
        private set

    var birthDate: LocalDate? = birthDate
        private set

    var school: String? = school
        private set

    var grade: String? = grade
        private set

    var memo: String? = memo
        private set

    fun rename(newName: String) {
        validateName(newName)
        name = newName
    }

    fun updateProfile(
        phone: String?,
        birthDate: LocalDate?,
        school: String?,
        grade: String?,
        memo: String?,
    ) {
        validateProfile(phone, birthDate, school, grade, memo)
        this.phone = phone
        this.birthDate = birthDate
        this.school = school
        this.grade = grade
        this.memo = memo
    }

    fun deactivate() {
        status = StudentStatus.INACTIVE
    }

    companion object {
        fun register(name: String): Student {
            validateName(name)
            return Student(
                id = TsidCreator.getTsid().toLong(),
                name = name,
                status = StudentStatus.ACTIVE,
                phone = null,
                birthDate = null,
                school = null,
                grade = null,
                memo = null,
            )
        }

        fun reconstitute(
            id: Long,
            name: String,
            status: StudentStatus,
            phone: String?,
            birthDate: LocalDate?,
            school: String?,
            grade: String?,
            memo: String?,
        ): Student {
            validateName(name)
            validateProfile(phone, birthDate, school, grade, memo)
            return Student(
                id = id,
                name = name,
                status = status,
                phone = phone,
                birthDate = birthDate,
                school = school,
                grade = grade,
                memo = memo,
            )
        }

        private fun validateName(name: String) {
            require(name.isNotBlank()) { "학생 이름은 비어 있을 수 없습니다." }
            require(name.length <= MAX_NAME_LENGTH) { "학생 이름은 100자를 초과할 수 없습니다." }
        }

        private fun validateProfile(
            phone: String?,
            birthDate: LocalDate?,
            school: String?,
            grade: String?,
            memo: String?,
        ) {
            require(phone == null || phone.length <= MAX_PHONE_LENGTH) { "학생 연락처는 20자를 초과할 수 없습니다." }
            require(birthDate == null || !birthDate.isAfter(LocalDate.now())) { "학생 생년월일은 미래일 수 없습니다." }
            require(school == null || school.length <= MAX_SCHOOL_LENGTH) { "학생 학교는 100자를 초과할 수 없습니다." }
            require(grade == null || grade.length <= MAX_GRADE_LENGTH) { "학생 학년은 20자를 초과할 수 없습니다." }
            require(memo == null || memo.length <= MAX_MEMO_LENGTH) { "학생 메모는 1000자를 초과할 수 없습니다." }
        }

        const val MAX_NAME_LENGTH = 100
        const val MAX_PHONE_LENGTH = 20
        const val MAX_SCHOOL_LENGTH = 100
        const val MAX_GRADE_LENGTH = 20
        const val MAX_MEMO_LENGTH = 1000
    }
}
