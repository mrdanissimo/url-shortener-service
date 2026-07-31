package com.mrdanissimo.shortener_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // Обработка ошибки, когда ссылка не найдена
    @ExceptionHandler(LinkNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Map<String, String> handleNotFound(LinkNotFoundException ex) {
        return Map.of("error", ex.getMessage());
    }

    // Обработка ошибки, когда срок ссылки истек
    @ExceptionHandler(LinkExpiredException.class)
    @ResponseStatus(HttpStatus.GONE)
    public Map<String, String> handleLinkExpired(LinkExpiredException ex) {
        return Map.of("error", ex.getMessage());
    }

    // Обработка ошибки, при неккоркектный вводе данных
    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Map<String, String> handleValidationExceptions(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        return Map.of("error", errorMessage != null ? errorMessage : "Некорректные данные");
    }
}
