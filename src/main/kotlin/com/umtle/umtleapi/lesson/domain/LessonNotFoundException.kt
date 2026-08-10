package com.umtle.umtleapi.lesson.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class LessonNotFoundException(
    val lessonId: Long,
) : AggregateNotFoundException("Lesson", lessonId)
