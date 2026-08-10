package com.umtle.umtleapi.lesson.domain

class InvalidLessonStateTransitionException(
    action: String,
    status: LessonStatus,
) : RuntimeException("Cannot $action a lesson in $status status")
