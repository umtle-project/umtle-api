package com.umtle.umtleapi.user.domain

import com.umtle.umtleapi.common.domain.AggregateNotFoundException

class UserNotFoundException(
    val userId: Long,
) : AggregateNotFoundException("User", userId)
