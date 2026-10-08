package com.project.order.exception

import lombok.Getter
import org.springframework.http.HttpStatus


enum class ErrorCode(val code: String, val status: HttpStatus, val defaultMessage: String) {
    UNAUTHORIZED("UNAUTHORIZED", HttpStatus.UNAUTHORIZED, "Для доступа к ресурсу необходима авторизация"),
    FORBIDDEN("FORBIDDEN", HttpStatus.FORBIDDEN, "Доступ запрещён"),
    BAD_REQUEST("BAD_REQUEST", HttpStatus.BAD_REQUEST, "Плохой хапрос"),
    VALIDATION_ERROR("VALIDATION_ERROR", HttpStatus.BAD_REQUEST, "Ошибка валидации данных"),
    ALREADY_EXISTS("ALREADY_EXISTS", HttpStatus.CONFLICT, "Запись уже существует в системе"),
    CONFLICT("CONFLICT", HttpStatus.CONFLICT, "Конфликт данных"),
    RESOURCE_NOT_FOUND("RESOURCE_NOT_FOUND", HttpStatus.NOT_FOUND, "Запись в базе данных не найдена"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR", HttpStatus.INTERNAL_SERVER_ERROR, "Внутренняя ошибка сервера");



}
