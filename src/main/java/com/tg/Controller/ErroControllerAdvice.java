package com.tg.Controller;

import java.util.Map;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ErroControllerAdvice {
  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<?> invalido(IllegalArgumentException e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", e.getMessage() == null ? "Dados inválidos" : e.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<?> validacao(MethodArgumentNotValidException e) {
    return ResponseEntity.badRequest()
        .body(
            Map.of(
                "message",
                "Revise os campos obrigatórios e seus formatos",
                "campos",
                e.getBindingResult().getFieldErrors().stream()
                    .map(x -> x.getField() + ": " + x.getDefaultMessage())
                    .toList()));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> status(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("message", e.getReason() == null ? "Requisição não atendida" : e.getReason()));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class
  })
  ResponseEntity<?> conflito(Exception e) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "message",
                "Conflito: registro duplicado, vinculado ou alterado por outro usuário. Recarregue"
                    + " os dados."));
  }

  @ExceptionHandler(MaxUploadSizeExceededException.class)
  ResponseEntity<?> tamanho(Exception e) {
    return ResponseEntity.status(413).body(Map.of("message", "O arquivo deve ter no máximo 5 MB"));
  }

  @ExceptionHandler(java.io.IOException.class)
  ResponseEntity<?> io(Exception e) {
    return ResponseEntity.status(500)
        .body(Map.of("message", "Não foi possível acessar ou gerar o arquivo"));
  }
}
