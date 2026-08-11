package com.umtle.umtleapi.homework.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class HomeworkNotFoundException(
    val homeworkId: Long,
) : AggregateNotFoundException("Homework", homeworkId)
