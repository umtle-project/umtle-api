package com.umtle.umtleapi.student.domain

import com.github.f4b6a3.tsid.TsidCreator

class Student private constructor(
    val id: Long,
    name: String,
    status: StudentStatus,
) {
    var name: String = name
        private set

    var status: StudentStatus = status
        private set

    fun rename(newName: String) {
        validateName(newName)
        name = newName
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
            )
        }

        fun reconstitute(
            id: Long,
            name: String,
            status: StudentStatus,
        ): Student = Student(id = id, name = name, status = status)

        private fun validateName(name: String) {
            require(name.isNotBlank()) { "학생 이름은 비어 있을 수 없습니다." }
            require(name.length <= MAX_NAME_LENGTH) { "학생 이름은 100자를 초과할 수 없습니다." }
        }

        const val MAX_NAME_LENGTH = 100
    }
}
