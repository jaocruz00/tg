package com.tg.Service;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import java.time.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class MinutaService {
  private final MinutaRepository minutas;
  private final ClienteRepository clientes;
  private final ModeloRepository modelos;
  private final ModeloVersaoRepository versoes;
  private final UsuarioRepository usuarios;
  private final ConfiguracaoService config;
  private final TemplateEngine engine;
  private final AuditoriaService auditoria;

  public MinutaEntity buscar(Long id) {
    return minutas
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Minuta não encontrada"));
  }

  @Transactional
  public MinutaEntity gerar(Long clienteId, String codigo) {
    var c =
        clientes
            .findById(clienteId)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado"));
    if (!c.isRevisado())
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "Revise e salve os dados do cliente antes de gerar");
    var m =
        modelos
            .findByCodigo(codigo)
            .filter(x -> x.getStatus().equals("PUBLICADO"))
            .orElseThrow(
                () ->
                    new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Modelo publicado não encontrado"));
    var v = versoes.findByModeloIdAndNumero(m.getId(), m.getNumeroVersao()).orElseThrow();
    var u = usuarios.findByEmail(AuditoriaService.ator()).orElseThrow();
    Map<String, String> d = new HashMap<>();
    d.put("cliente.nome", c.getNome());
    d.put("cliente.cpf", c.getCpf());
    d.put("cliente.rg", c.getRg());
    d.put("cliente.filiacao", c.getFiliacao());
    d.put(
        "cliente.dataNascimento",
        c.getDataNascimento() == null ? null : c.getDataNascimento().toString());
    d.put("cliente.orgaoExpedidor", c.getOrgaoExpedidor());
    d.put("cliente.endereco", c.getEndereco());
    d.put("cliente.numero", c.getNumero());
    d.put("cliente.bairro", c.getBairro());
    d.put("cliente.cidade", c.getCidade());
    d.put("cliente.estado", c.getEstado());
    d.put("cliente.cep", c.getCep());
    d.put("cliente.telefone", c.getTelefone());
    d.put("cliente.email", c.getEmail());
    d.put("advogado.nome", u.getNome());
    d.put("advogado.oab", u.getOab());
    config.escritorio().forEach((k, val) -> d.put("escritorio." + k, val));
    d.put("documento.data", LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString());
    var minuta = new MinutaEntity();
    minuta.setCliente(c);
    minuta.setModeloVersao(v);
    minuta.setConteudo(engine.preencher(v.getConteudo(), d));
    minuta.setAutor(u.getEmail());
    minuta.setCriadoEm(Instant.now());
    minutas.saveAndFlush(minuta);
    auditoria.registrar("DOCUMENTOS", "GERAR_MINUTA", minuta.getId().toString());
    return minuta;
  }

  @Transactional
  public MinutaEntity revisar(Long id, String texto, Long revisao, boolean finalizar) {
    var m = buscar(id);
    if (!Objects.equals(m.getRevisao(), revisao))
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Minuta alterada; recarregue");
    if (m.getStatus().equals("FINALIZADO"))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT,
          "Documento finalizado não pode ser sobrescrito; gere uma nova minuta");
    if (texto.contains("{{") || texto.contains("}}"))
      throw new IllegalArgumentException("Há marcadores não preenchidos");
    m.setConteudo(texto);
    m.setStatus(finalizar ? "FINALIZADO" : "RASCUNHO");
    if (finalizar) {
      m.setRevisor(AuditoriaService.ator());
      m.setRevisadoEm(Instant.now());
    }
    minutas.saveAndFlush(m);
    auditoria.registrar("DOCUMENTOS", finalizar ? "FINALIZAR" : "REVISAR", id.toString());
    return m;
  }
}
