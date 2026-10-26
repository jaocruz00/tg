package com.tg.Controller;

import com.tg.Service.AuditoriaService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
public class EventoController {
  private final AuditoriaService auditoria;

  public record Evento(
      @NotBlank
          @Pattern(regexp = "EXTRACAO|CLIENTES|DOCUMENTOS|MODELOS|USUARIOS|AUDITORIA|CONFIGURACOES")
          String modulo,
      @NotBlank @Pattern(regexp = "ABRIR_TELA|CANCELAR_EDICAO|ERRO_INTERFACE") String acao,
      @Size(max = 80) String registro) {}

  @PostMapping
  public void evento(@Valid @RequestBody Evento e) {
    auditoria.registrar(e.modulo(), e.acao(), e.registro(), "FRONTEND_NAO_VERIFICADO");
  }
}
