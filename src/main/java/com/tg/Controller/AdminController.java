package com.tg.Controller;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import com.tg.Service.*;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {
  private final UsuarioRepository usuarios;
  private final UsuarioService usuarioService;
  private final AuditoriaRepository logs;
  private final ClienteRepository clientes;
  private final ModeloRepository modelos;
  private final MinutaRepository minutas;
  private final ResultadoOCRRepository resultados;
  private final ConfiguracaoService config;

  @GetMapping("/usuarios")
  public Page<UsuarioEntity> usuarios(@RequestParam(defaultValue="0") int page) {
    return usuarios.findAll(PageRequest.of(Math.max(0, page), 50));
  }

  @PostMapping("/usuarios")
  public UsuarioEntity criar(@Valid @RequestBody UsuarioService.Entrada d) {
    return usuarioService.salvar(null, d);
  }

  @PutMapping("/usuarios/{id}")
  public UsuarioEntity editar(@PathVariable Long id, @Valid @RequestBody UsuarioService.Entrada d) {
    return usuarioService.salvar(id, d);
  }

  @GetMapping("/modelos")
  public Page<ModeloEntity> modelos(@RequestParam(defaultValue="0") int page) {
    return modelos.findAll(PageRequest.of(Math.max(0, page), 50));
  }

  @GetMapping("/painel")
  public Object painel() {
    return Map.of("clientes", clientes.count(),
        "pendentesRevisao", clientes.countByRevisadoFalse(),
        "documentosGerados", minutas.count(),
        "modelosPublicados", modelos.countByStatus("PUBLICADO"),
        "extracoesComErro", resultados.countByStatus("ERRO"),
        "extracoesProcessando", resultados.countByStatus("PROCESSANDO"));
  }

  @GetMapping("/auditoria")
  public Page<AuditoriaEntity> auditoria(
      @RequestParam(required = false) String usuario,
      @RequestParam(required = false) String modulo,
      @RequestParam(required = false) String acao,
      @RequestParam(required = false) Instant inicio,
      @RequestParam(required = false) Instant fim,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "30") int size) {
    Specification<AuditoriaEntity> spec =
        (root, q, cb) -> {
          List<Predicate> p = new ArrayList<>();
          if (usuario != null) p.add(cb.equal(root.get("usuario"), usuario));
          if (modulo != null) p.add(cb.equal(root.get("modulo"), modulo));
          if (acao != null) p.add(cb.equal(root.get("acao"), acao));
          if (inicio != null) p.add(cb.greaterThanOrEqualTo(root.get("dataHora"), inicio));
          if (fim != null) p.add(cb.lessThanOrEqualTo(root.get("dataHora"), fim));
          return cb.and(p.toArray(Predicate[]::new));
        };
    return logs.findAll(
        spec,
        PageRequest.of(
            Math.max(0, page), Math.min(100, Math.max(1, size)), Sort.by("id").descending()));
  }

  @GetMapping("/escritorio")
  public Object escritorio() {
    return config.escritorio();
  }

  @PutMapping("/escritorio")
  public void escritorio(@RequestBody Map<String, String> dados) {
    config.escritorio(dados);
  }

  public record ModeloIA(@NotBlank String modelo) {}

  @GetMapping("/ia")
  public Object ia() {
    return Map.of("modelo", config.modelo(), "modelosPermitidos", config.modelos());
  }

  @PutMapping("/ia")
  public void ia(@Valid @RequestBody ModeloIA d) {
    config.modelo(d.modelo());
  }
}
