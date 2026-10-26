package com.tg.Service;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import java.nio.file.Path;
import java.time.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
@RequiredArgsConstructor
public class PersistenciaOCRService {
  private final ClienteRepository clientes;
  private final DocumentoRepository documentos;
  private final ResultadoOCRRepository resultados;
  private final AuditoriaService auditoria;

  public static String texto(JsonNode j, String k) {
    var n = j.get(k);
    return n == null || n.isNull() || !n.isValueNode() ? null : n.asText().trim();
  }

  private ClienteEntity atualizar(JsonNode j, ClienteEntity vinculado) {
    String cpf = DadosDocumento.cpf(texto(j, "cpf"));
    String nome = texto(j, "nome");
    if (nome == null || nome.isBlank() || nome.length() > 255)
      throw new IllegalArgumentException("Nome não identificado. Envie documento legível");
    if (vinculado != null && !cpf.equals(vinculado.getCpf()))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "CPF extraído difere do cliente vinculado");
    var c = vinculado != null ? vinculado : clientes.findByCpf(cpf).orElseGet(ClienteEntity::new);
    c.setCpf(cpf);
    c.setNome(nome);
    String rg = texto(j, "registro_geral"),
        orgao = texto(j, "orgao_expedidor"),
        filiacao = texto(j, "filiacao");
    if (rg != null && !rg.isBlank()) {
      if (rg.length() > 20) throw new IllegalArgumentException("RG extraído inválido");
      c.setRg(rg);
    }
    if (orgao != null && !orgao.isBlank()) {
      if (orgao.length() > 20)
        throw new IllegalArgumentException("Órgão expedidor extraído inválido");
      c.setOrgaoExpedidor(orgao);
    }
    if (filiacao != null && !filiacao.isBlank()) {
      if (filiacao.length() > 255) throw new IllegalArgumentException("Filiação extraída inválida");
      c.setFiliacao(filiacao);
    }
    var data = DadosDocumento.data(texto(j, "data_nascimento"));
    if (data != null) c.setDataNascimento(data);
    c.setRevisado(false);
    return clientes.saveAndFlush(c);
  }

  @Transactional
  public ClienteEntity salvar(JsonNode j, ArquivoService.Arquivo a, Path path) {
    ClienteEntity c = atualizar(j, null);
    DocumentoEntity d = new DocumentoEntity();
    d.setNomeOriginal(a.nome());
    d.setNomeArmazenado(path.getFileName().toString());
    d.setCaminhoArquivo(path.toString());
    d.setTipoArquivo(a.tipo());
    d.setTamanhoArquivo((long) a.bytes().length);
    d.setDataUpload(LocalDateTime.now());
    d.setCliente(c);
    documentos.saveAndFlush(d);
    gravarResultado(d, j);
    auditoria.registrar("OCR", "EXTRAIR_E_SALVAR", d.getId().toString());
    return c;
  }

  private ResultadoOCREntity gravarResultado(DocumentoEntity d, JsonNode j) {
    var r = resultados.findByDocumentoId(d.getId()).orElseGet(ResultadoOCREntity::new);
    r.setDocumento(d);
    r.setResultadoJson(j.toString());
    r.setStatus("PROCESSADO");
    r.setDataProcessamento(LocalDateTime.now());
    return resultados.save(r);
  }

  @Transactional
  public ResultadoOCREntity reprocessar(Long id, JsonNode j) {
    var d =
        documentos
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    atualizar(j, d.getCliente());
    var r = gravarResultado(d, j);
    auditoria.registrar("OCR", "REPROCESSAR", id.toString());
    return r;
  }

  @Transactional
  public void estado(Long id, String estado) {
    var d =
        documentos
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    var r = resultados.findByDocumentoId(id).orElseGet(ResultadoOCREntity::new);
    r.setDocumento(d);
    r.setStatus(estado);
    r.setDataProcessamento(LocalDateTime.now());
    resultados.save(r);
  }
}
