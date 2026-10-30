package com.tg.Service;

import com.tg.Entities.ConfiguracaoEntity;
import com.tg.Repositories.ConfiguracaoRepository;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ConfiguracaoService {
  private final ConfiguracaoRepository repo;
  private final AuditoriaService auditoria;

  @Value("${app.ollama.modelo}")
  private String modeloPadrao;

  @Value("${app.ollama.allowed-models}")
  private String modelosPermitidos;

  public String modelo() {
    return repo.findByChave("ia.modelo").map(ConfiguracaoEntity::getValor).orElse(modeloPadrao);
  }

  public List<String> modelos() {
    return Arrays.stream(modelosPermitidos.split(",")).map(String::trim).toList();
  }

  public Map<String, String> escritorio() {
    Map<String, String> out = new LinkedHashMap<>();
    for (String k : List.of("nome", "endereco", "telefone", "email")) out.put(k, "");
    repo.findByChaveStartingWith("escritorio.").forEach(c -> out.computeIfPresent(
        c.getChave().substring("escritorio.".length()), (k, v) -> c.getValor() == null ? "" : c.getValor()));
    return out;
  }

  private void salvar(String chave, String valor) {
    var c = repo.findByChave(chave).orElseGet(ConfiguracaoEntity::new);
    c.setChave(chave);
    c.setValor(valor);
    repo.save(c);
  }

  @Transactional
  public void modelo(String modelo) {
    if (!modelos().contains(modelo))
      throw new IllegalArgumentException("Modelo fora da lista permitida no servidor");
    salvar("ia.modelo", modelo);
    auditoria.registrar("IA", "CONFIGURAR_MODELO", modelo);
  }

  @Transactional
  public void escritorio(Map<String, String> dados) {
    for (var e : dados.entrySet()) {
      if (!List.of("nome", "endereco", "telefone", "email").contains(e.getKey())
          || e.getValue() == null
          || e.getValue().length() > 500)
        throw new IllegalArgumentException("Configuração de escritório inválida");
      salvar("escritorio." + e.getKey(), e.getValue());
    }
    auditoria.registrar("ESCRITORIO", "ATUALIZAR", null);
  }
}
