package com.project.order.exception

class NotFoundOrderException : RuntimeException {
    constructor(message: String) : super(message)
}