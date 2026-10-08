package com.project.order.exception.variants

import com.project.order.exception.BaseException
import com.project.order.exception.ErrorCode

class OrderCreationException(message: String):
    BaseException(
        message,
        ErrorCode.INTERNAL_SERVER_ERROR.code,
        ErrorCode.INTERNAL_SERVER_ERROR.status
    )