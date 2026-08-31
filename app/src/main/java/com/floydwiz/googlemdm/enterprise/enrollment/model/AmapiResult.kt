package com.floydwiz.googlemdm.enterprise.enrollment.model

sealed class AmapiResult<out T> {
    data class Success<T>(val data: T) : AmapiResult<T>()
    data class Failure(val message: String) : AmapiResult<Nothing>()
}
