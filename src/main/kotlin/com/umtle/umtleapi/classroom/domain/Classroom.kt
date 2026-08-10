package com.umtle.umtleapi.classroom.domain

import com.github.f4b6a3.tsid.TsidCreator

class Classroom private constructor(
    val id: Long,
    name: String,
    status: ClassroomStatus,
    studentIds: Set<Long>,
    teacherIds: Set<Long>,
) {
    var name: String = name
        private set

    var status: ClassroomStatus = status
        private set

    var studentIds: Set<Long> = studentIds
        private set

    var teacherIds: Set<Long> = teacherIds
        private set

    fun rename(newName: String) {
        validateName(newName)
        name = newName
    }

    fun deactivate() {
        status = ClassroomStatus.INACTIVE
    }

    fun assignStudent(studentId: Long) {
        studentIds = studentIds + studentId
    }

    fun unassignStudent(studentId: Long) {
        studentIds = studentIds - studentId
    }

    fun assignTeacher(teacherId: Long) {
        teacherIds = teacherIds + teacherId
    }

    fun unassignTeacher(teacherId: Long) {
        teacherIds = teacherIds - teacherId
    }

    companion object {
        fun register(name: String): Classroom {
            validateName(name)
            return Classroom(
                id = TsidCreator.getTsid().toLong(),
                name = name,
                status = ClassroomStatus.ACTIVE,
                studentIds = emptySet(),
                teacherIds = emptySet(),
            )
        }

        fun reconstitute(
            id: Long,
            name: String,
            status: ClassroomStatus,
            studentIds: Set<Long>,
            teacherIds: Set<Long>,
        ): Classroom {
            validateName(name)
            return Classroom(
                id = id,
                name = name,
                status = status,
                studentIds = studentIds,
                teacherIds = teacherIds,
            )
        }

        private fun validateName(name: String) {
            require(name.isNotBlank()) { "반 이름은 비어 있을 수 없습니다." }
            require(name.length <= MAX_NAME_LENGTH) { "반 이름은 100자를 초과할 수 없습니다." }
        }

        const val MAX_NAME_LENGTH = 100
    }
}
