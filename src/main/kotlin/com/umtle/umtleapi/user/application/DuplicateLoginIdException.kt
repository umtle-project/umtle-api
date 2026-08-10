package com.umtle.umtleapi.user.application

class DuplicateLoginIdException(
    loginId: String,
) : RuntimeException("Duplicate loginId: $loginId")
