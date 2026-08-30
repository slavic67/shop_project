package com.project.order

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.util.stream.Collectors

@RestControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleException(ex: MethodArgumentNotValidException) : ResponseEntity<ErrorDto> {

        val errors: List<String> = ex.getFieldErrors()
            .stream().map { error-> error.field + ":" + error.defaultMessage }
            .collect(Collectors.toList())

        return ResponseEntity.badRequest().body(
            ErrorDto(ex.statusCode.value(), "Validation Failed", errors)
        )
    }
}