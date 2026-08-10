package com.umtle.umtleapi.classroom.presentation

import com.umtle.umtleapi.classroom.application.ClassroomService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/classes")
class ClassroomController(
    private val classroomService: ClassroomService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterClassroomRequest,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.registerClass(request.name))

    @GetMapping
    fun list(): List<ClassroomResponse> = classroomService.listClasses().map { ClassroomResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.getClass(id))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateClassroomRequest,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.updateClass(id, request.name))

    @PostMapping("/{id}/deactivate")
    fun deactivate(
        @PathVariable id: Long,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.deactivateClass(id))

    @PostMapping("/{id}/students")
    fun assignStudent(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignStudentRequest,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.assignStudent(id, request.studentId))

    @DeleteMapping("/{id}/students/{studentId}")
    fun unassignStudent(
        @PathVariable id: Long,
        @PathVariable studentId: Long,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.unassignStudent(id, studentId))

    @PostMapping("/{id}/teachers")
    fun assignTeacher(
        @PathVariable id: Long,
        @Valid @RequestBody request: AssignTeacherRequest,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.assignTeacher(id, request.teacherId))

    @DeleteMapping("/{id}/teachers/{teacherId}")
    fun unassignTeacher(
        @PathVariable id: Long,
        @PathVariable teacherId: Long,
    ): ClassroomResponse = ClassroomResponse.from(classroomService.unassignTeacher(id, teacherId))
}
