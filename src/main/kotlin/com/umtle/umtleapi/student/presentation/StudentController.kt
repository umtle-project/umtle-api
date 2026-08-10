package com.umtle.umtleapi.student.presentation

import com.umtle.umtleapi.student.application.StudentService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/students")
class StudentController(
    private val studentService: StudentService,
) {
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun register(
        @Valid @RequestBody request: RegisterStudentRequest,
    ): StudentResponse = StudentResponse.from(studentService.registerStudent(request.name))

    @GetMapping
    fun list(): List<StudentResponse> = studentService.listStudents().map { StudentResponse.from(it) }

    @GetMapping("/{id}")
    fun get(
        @PathVariable id: Long,
    ): StudentResponse = StudentResponse.from(studentService.getStudent(id))

    @PatchMapping("/{id}")
    fun update(
        @PathVariable id: Long,
        @Valid @RequestBody request: UpdateStudentRequest,
    ): StudentResponse = StudentResponse.from(studentService.updateStudent(id, request.name))

    @PostMapping("/{id}/deactivate")
    fun deactivate(
        @PathVariable id: Long,
    ): StudentResponse = StudentResponse.from(studentService.deactivateStudent(id))
}
