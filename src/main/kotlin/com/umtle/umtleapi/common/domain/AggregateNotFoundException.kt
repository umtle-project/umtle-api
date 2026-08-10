package com.umtle.umtleapi.common.domain

abstract class AggregateNotFoundException(
    aggregateType: String,
    id: Any,
) : RuntimeException("$aggregateType not found: $id")
