package com.umtle.umtleapi.learningrecord.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class LearningRecordNotFoundException(
    id: Long,
) : AggregateNotFoundException("LearningRecord", id)
