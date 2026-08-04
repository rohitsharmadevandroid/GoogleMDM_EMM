package com.floydwiz.googlemdm.core.logger

import timber.log.Timber

object Logger {

    fun d(message: String) {
        Timber.d(message)
    }

    fun e(message: String) {
        Timber.e(message)
    }

    fun i(message: String) {
        Timber.i(message)
    }

    fun w(message: String) {
        Timber.w(message)
    }
}