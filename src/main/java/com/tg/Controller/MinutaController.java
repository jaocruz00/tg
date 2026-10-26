package com.tg.Controller;

import com.tg.Entities.MinutaEntity;
import com.tg.Repositories.MinutaRepository;
import com.tg.Service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/minutas")
@RequiredArgsConstructor
public class MinutaController {
  private final MinutaService service;
  private final MinutaRepository repo;
  private final ExportacaoService exportacao;
  private final AuditoriaService auditoria;

  public record Gerar(@NotNull Long clienteId, @NotBlank String modeloCodigo) {}

  public record Revisar(
      @NotBlank @Size(max = 100000) String conteudo, @NotNull Long revisao, boolean finalizar) {}

  @PostMapping
  public MinutaEntity gerar(@Valid @RequestBody Gerar d) {
    return service.gerar(d.clienteId(), d.modeloCodigo());
  }

  @GetMapping
  public List<MinutaEntity> listar(
      @RequestParam(required = false) Long clienteId,
      @RequestParam(defaultValue = "") String busca) {
    String q = busca.toLowerCase(Locale.ROOT);
    return repo.findAll().stream()
        .filter(m -> clienteId == null || m.getCliente().getId().equals(clienteId))
        .filter(
            m ->
                q.isBlank()
                    || m.getConteudo().toLowerCase(Locale.ROOT).contains(q)
                    || m.getCliente().getNome().toLowerCase(Locale.ROOT).contains(q))
        .toList();
  }

  @GetMapping("/{id}")
  public MinutaEntity buscar(@PathVariable Long id) {
    return service.buscar(id);
  }

  @PutMapping("/{id}")
  public MinutaEntity revisar(@PathVariable Long id, @Valid @RequestBody Revisar d) {
    return service.revisar(id, d.conteudo(), d.revisao(), d.finalizar());
  }

  @GetMapping("/{id}/arquivo")
  public ResponseEntity<byte[]> arquivo(
      @PathVariable Long id, @RequestParam(defaultValue = "pdf") String formato)
      throws IOException {
    var m = service.buscar(id);
    if (!m.getStatus().equals("FINALIZADO"))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Revise e finalize antes de exportar");
    byte[] bytes;
    String tipo;
    switch (formato) {
      case "pdf" -> {
        bytes = exportacao.pdf(m.getConteudo());
        tipo = "application/pdf";
      }
      case "docx" -> {
        bytes = exportacao.docx(m.getConteudo());
        tipo = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
      }
      case "txt" -> {
        bytes = exportacao.txt(m.getConteudo());
        tipo = "text/plain;charset=UTF-8";
      }
      default -> throw new IllegalArgumentException("Formato deve ser pdf, docx ou txt");
    }
    auditoria.registrar(
        "DOCUMENTOS", "EXPORTAR_" + formato.toUpperCase(Locale.ROOT), id.toString());
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(tipo))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment; filename=\"documento-" + id + "." + formato + "\"")
        .body(bytes);
  }
}
