package br.com.salgadosdalucia.api.handler;

import br.com.salgadosdalucia.api.exception.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationServiceException;
import org.springframework.security.authentication.ProviderNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalHandlerException {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Falha inesperada ao processar {}", request.getRequestURI(), ex);
        return buildResponse("Ocorreu um erro interno. Tente novamente mais tarde.", HttpStatus.INTERNAL_SERVER_ERROR, request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                     HttpServletRequest request) {
        String mensagem = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErroResponse erroResponse = new ErroResponse(
                mensagem,
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST,
                request.getRequestURI(),
                LocalDateTime.now());

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(erroResponse);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErroResponse> handleHttpMessageNotReadable(HttpServletRequest request) {
        return buildResponse("Corpo da requisição inválido. Verifique os dados enviados.", HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErroResponse> handleNotFound(NotFoundException ex, HttpServletRequest request) {
        return buildResponse(ex.getMessage(), HttpStatus.NOT_FOUND, request);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErroResponse> handleBadRequest(BadRequestException ex, HttpServletRequest request) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErroResponse> handleBusiness(BusinessException ex, HttpServletRequest request) {
        return buildResponse(ex.getMessage(), HttpStatus.BAD_REQUEST, request);
    }

    // 403 proibido
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErroResponse> handleAccessDenied(HttpServletRequest request) {
        return buildResponse("Acesso negado.", HttpStatus.FORBIDDEN, request);
    }

    @ExceptionHandler({AuthenticationServiceException.class, ProviderNotFoundException.class})
    public ResponseEntity<ErroResponse> handleAuthenticationService(AuthenticationException ex, HttpServletRequest request) {
        log.error("Falha técnica na autenticação em {}", request.getRequestURI(), ex);
        return buildResponse("Não foi possível autenticar no momento. Tente novamente mais tarde.", HttpStatus.SERVICE_UNAVAILABLE, request);
    }

    // 401 não autorizado
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErroResponse> handleAuthentication(HttpServletRequest request) {
        return buildResponse("Usuário ou senha inválidos.", HttpStatus.UNAUTHORIZED, request);
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErroResponse> handleInvalidCredentials(InvalidCredentialsException ex, HttpServletRequest request) {
        return buildResponse(ex.getMessage(), HttpStatus.UNAUTHORIZED, request);
    }

    // metodo auxiliar para evitar repetição de código
    private ResponseEntity<ErroResponse> buildResponse(String msg, HttpStatus status, HttpServletRequest req) {
        ErroResponse erro = new ErroResponse(
                msg,
                status.value(),
                status,
                req.getRequestURI(),
                LocalDateTime.now());
        return ResponseEntity.status(status).body(erro);
    }

}
