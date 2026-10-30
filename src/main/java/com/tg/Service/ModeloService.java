package com.tg.Service;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class ModeloService {
  private final ModeloRepository modelos;
  private final ModeloVersaoRepository versoes;
  private final TemplateEngine engine;
  private final AuditoriaService auditoria;

  public record Entrada(
      @NotBlank @Pattern(regexp = "[a-z0-9_-]{1,80}") String codigo,
      @NotBlank @Size(max = 255) String nome,
      @Size(max = 255) String categoria,
      @Size(max = 255) String area,
      @Size(max = 255) String tags,
      @Size(max = 255) String descricao,
      @NotBlank @Size(max = 100000) String conteudo,
      Long revisao) {}

  public ModeloEntity buscar(Long id) {
    return modelos
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Modelo não encontrado"));
  }

  @Transactional
  public ModeloEntity salvar(Long id, Entrada d) {
    ModeloEntity m = id == null ? new ModeloEntity() : buscar(id);
    if (id != null && !Objects.equals(m.getRevisao(), d.revisao()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Modelo alterado; recarregue antes de salvar");
    modelos
        .findByCodigo(d.codigo())
        .filter(x -> !Objects.equals(x.getId(), id))
        .ifPresent(
            x -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "Código já utilizado");
            });
    m.setCodigo(d.codigo());
    m.setNome(d.nome());
    m.setCategoria(d.categoria());
    m.setArea(d.area());
    m.setTags(d.tags());
    m.setDescricao(d.descricao());
    m.setConteudo(d.conteudo());
    m.setStatus("RASCUNHO");
    m.setAutor(AuditoriaService.ator());
    m.setAtualizadoEm(Instant.now());
    modelos.saveAndFlush(m);
    auditoria.registrar("MODELOS", "SALVAR_RASCUNHO", m.getId().toString());
    return m;
  }

  @Transactional
  public ModeloEntity publicar(Long id, Long revisao) {
    var m = buscar(id);
    if (!Objects.equals(revisao, m.getRevisao()))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Modelo alterado; recarregue");
    engine.validar(m.getConteudo());
    m.setNumeroVersao(m.getNumeroVersao() + 1);
    m.setStatus("PUBLICADO");
    m.setAtualizadoEm(Instant.now());
    modelos.saveAndFlush(m);
    var v = new ModeloVersaoEntity();
    v.setModelo(m);
    v.setNumero(m.getNumeroVersao());
    v.setConteudo(m.getConteudo());
    v.setAutor(AuditoriaService.ator());
    v.setCriadoEm(Instant.now());
    versoes.save(v);
    auditoria.registrar("MODELOS", "PUBLICAR", id + "/v" + v.getNumero());
    return m;
  }

  @Transactional
  public void inativar(Long id) {
    var m = buscar(id);
    m.setStatus("INATIVO");
    m.setAtualizadoEm(Instant.now());
    auditoria.registrar("MODELOS", "INATIVAR", id.toString());
  }

  @Transactional
  public ModeloEntity duplicar(Long id, String codigo) {
    var m = buscar(id);
    return salvar(
        null,
        new Entrada(
            codigo,
            m.getNome() + " (cópia)",
            m.getCategoria(),
            m.getArea(),
            m.getTags(),
            m.getDescricao(),
            m.getConteudo(),
            null));
  }
}
