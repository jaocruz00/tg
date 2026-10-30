package com.tg.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;
@RestControllerAdvice
public class ApiErrors {
 @ExceptionHandler(IllegalArgumentException.class)
 ResponseEntity<ProblemDetail> invalid(IllegalArgumentException e) {
   return ResponseEntity.badRequest().body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,"Dados inválidos"));
 }
 @ExceptionHandler({org.springframework.dao.DataIntegrityViolationException.class,org.springframework.orm.ObjectOptimisticLockingFailureException.class})
 ResponseEntity<ProblemDetail> conflict(Exception e) {
   return ResponseEntity.status(409).body(ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,"Registro em conflito ou relacionado a outros dados"));
 }
 @ExceptionHandler(IllegalStateException.class)
 ResponseEntity<ProblemDetail> upstream(IllegalStateException e) {
   return ResponseEntity.status(502).body(ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,"Falha no processamento externo"));
 }
}
