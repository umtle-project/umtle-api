package com.umtle.umtleapi.common.domain

import org.springframework.http.HttpStatus

abstract class AggregateNotFoundException(
    aggregateType: String,
    id: Any,
) : UmtleCustomException("$aggregateType not found: $id", HttpStatus.NOT_FOUND)
