package com.tg.Controller;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import com.tg.Service.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/modelos")
@RequiredArgsConstructor
public class ModeloController {
  private final ModeloService service;
  private final ModeloRepository repo;
  private final ModeloVersaoRepository versoes;
  private final TemplateEngine engine;

  public record Versao(@NotNull Long revisao) {}

  public record Copia(@NotBlank @Pattern(regexp = "[a-z0-9_-]{1,80}") String codigo) {}

  public record Preview(@NotBlank @Size(max = 100000) String conteudo) {}

  @GetMapping
  public List<ModeloEntity> publicados() {
    return repo.findAll().stream().filter(m -> m.getStatus().equals("PUBLICADO")).toList();
  }

  @GetMapping("/campos")
  public Set<String> campos() {
    return TemplateEngine.CAMPOS;
  }

  @PostMapping
  @PreAuthorize("hasRole('ADMIN')")
  public ModeloEntity criar(@Valid @RequestBody ModeloService.Entrada d) {
    return service.salvar(null, d);
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasRole('ADMIN')")
  public ModeloEntity editar(@PathVariable Long id, @Valid @RequestBody ModeloService.Entrada d) {
    return service.salvar(id, d);
  }

  @PostMapping("/{id}/publicar")
  @PreAuthorize("hasRole('ADMIN')")
  public ModeloEntity publicar(@PathVariable Long id, @Valid @RequestBody Versao d) {
    return service.publicar(id, d.revisao());
  }

  @PostMapping("/{id}/inativar")
  @PreAuthorize("hasRole('ADMIN')")
  public void inativar(@PathVariable Long id) {
    service.inativar(id);
  }

  @PostMapping("/{id}/duplicar")
  @PreAuthorize("hasRole('ADMIN')")
  public ModeloEntity duplicar(@PathVariable Long id, @Valid @RequestBody Copia d) {
    return service.duplicar(id, d.codigo());
  }

  @GetMapping("/{id}/versoes")
  @PreAuthorize("hasRole('ADMIN')")
  public List<ModeloVersaoEntity> versoes(@PathVariable Long id) {
    return versoes.findByModeloIdOrderByNumeroDesc(id);
  }

  @PostMapping("/validar")
  @PreAuthorize("hasRole('ADMIN')")
  public Object validar(@Valid @RequestBody Preview d) {
    return Map.of("camposObrigatorios", engine.validar(d.conteudo()));
  }

  @PostMapping("/preview")
  @PreAuthorize("hasRole('ADMIN')")
  public Object preview(@Valid @RequestBody Preview d) {
    Map<String, String> exemplo = new HashMap<>();
    TemplateEngine.CAMPOS.forEach(k -> exemplo.put(k, "[Exemplo: " + k + "]"));
    return Map.of("conteudo", engine.preencher(d.conteudo(), exemplo));
  }
}
