package com.project.order

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class ErrorDto (
    val status: Int,
    val code: String,
    val details: Any
) {
}