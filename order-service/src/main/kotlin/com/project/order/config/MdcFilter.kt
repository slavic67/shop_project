package com.project.order.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.MDC
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
class MdcFilter : OncePerRequestFilter() {


    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        try {
            MDC.put("method", request.method)
            MDC.put("path", request.requestURI)
            MDC.put("client_ip", request.remoteAddr)
            filterChain.doFilter(request, response)
        } finally {
            // очищаем весь MDC, чтобы контекст не утёк в другой запрос
            MDC.clear()
        }
    }

}