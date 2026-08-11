package com.floydwiz.googlemdm.enterprise.enrollment.handler

import com.floydwiz.googlemdm.enterprise.enrollment.manager.EnrollmentManager
import com.floydwiz.googlemdm.enterprise.enrollment.model.EnrollmentState
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnrollmentHandler @Inject constructor(
    private val enrollmentManager: EnrollmentManager
) {
    fun getEnrollmentState(): EnrollmentState {
        return enrollmentManager.getEnrollmentState()
    }
}