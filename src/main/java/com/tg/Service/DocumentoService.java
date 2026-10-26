package com.tg.Service;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class DocumentoService {
  private final DocumentoRepository documentos;
  private final ArquivoService arquivos;
  private final OllamaClient ollama;
  private final PersistenciaOCRService persistencia;
  private final AuditoriaService auditoria;

  public ClienteEntity gerarCliente(MultipartFile file) throws IOException {
    var a = arquivos.validar(file);
    try {
      var dados = ollama.extrair(a.bytes(), a.tipo());
      Path p = arquivos.salvar(a);
      try {
        return persistencia.salvar(dados, a, p);
      } catch (RuntimeException e) {
        Files.deleteIfExists(p);
        throw e;
      }
    } catch (RuntimeException | IOException e) {
      auditoria.registrar("OCR", "FALHA_EXTRACAO", null);
      throw e;
    }
  }

  public List<DocumentoEntity> listarPorCliente(Long id) {
    return documentos.findByClienteId(id);
  }

  public DocumentoEntity buscarPorId(Long id) {
    return documentos
        .findById(id)
        .orElseThrow(
            () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Documento não encontrado"));
  }

  public Path caminho(Long id) {
    return arquivos.caminho(buscarPorId(id).getCaminhoArquivo());
  }

  public void deletar(Long id) {
    throw new ResponseStatusException(
        HttpStatus.CONFLICT,
        "Exclusão física indisponível: documento compõe o histórico de extração");
  }
}
