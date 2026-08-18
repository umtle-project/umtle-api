package com.umtle.umtleapi.user.domain

interface PendingUserQuery {
    fun findForApprover(
        loginId: String,
        approverRoles: Set<UserRole>,
        pendingRoles: Set<UserRole>,
    ): List<User>?
}
