package br.com.salgadosdalucia.api.exception;

import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

public record ErroResponse(
        String message,
        Integer status,
        HttpStatus httpStatus,
        String path,
        LocalDateTime dateTime
) {
}
