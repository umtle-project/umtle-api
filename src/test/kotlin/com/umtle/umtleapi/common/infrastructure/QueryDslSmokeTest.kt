package com.umtle.umtleapi.common.infrastructure

import com.querydsl.jpa.impl.JPAQueryFactory
import com.umtle.umtleapi.TestcontainersConfiguration
import com.umtle.umtleapi.student.domain.Student
import com.umtle.umtleapi.student.domain.StudentRepository
import com.umtle.umtleapi.student.infrastructure.QStudentJpaEntity.studentJpaEntity
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestcontainersConfiguration::class)
class QueryDslSmokeTest {
    @Autowired
    private lateinit var queryFactory: JPAQueryFactory

    @Autowired
    private lateinit var studentRepository: StudentRepository

    @Test
    fun `querydsl infrastructure can query generated q classes`() {
        val before = countStudents()

        studentRepository.save(Student.register("QueryDSL 스모크"))

        assertEquals(before + 1, countStudents())
    }

    private fun countStudents(): Long =
        queryFactory
            .select(studentJpaEntity.count())
            .from(studentJpaEntity)
            .fetchOne() ?: 0L
}
