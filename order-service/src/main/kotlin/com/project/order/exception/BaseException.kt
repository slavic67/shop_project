package com.project.order.exception

import org.springframework.http.HttpStatus

open class BaseException(
    message: String,
    val errorCode: String,
    val status : HttpStatus
) : RuntimeException(message)