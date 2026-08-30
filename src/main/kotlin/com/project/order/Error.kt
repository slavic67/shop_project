package com.project.order

import com.fasterxml.jackson.annotation.JsonInclude

@JsonInclude(JsonInclude.Include.NON_NULL)
data class Error (
    val status: Int,
    val code: String,
    val details: Any
) {
}