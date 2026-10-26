package com.tg.Service;

import com.tg.DTO.OCRResponseDTO;
import com.tg.Entities.ResultadoOCREntity;
import com.tg.Repositories.ResultadoOCRRepository;
import java.io.IOException;
import java.nio.file.Files;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class OCRService {
  private final DocumentoService documentos;
  private final ResultadoOCRRepository resultados;
  private final PersistenciaOCRService persistencia;
  private final OllamaClient ollama;

  public OCRResponseDTO processarDocumento(Long id) throws IOException {
    var d = documentos.buscarPorId(id);
    persistencia.estado(id, "PROCESSANDO");
    try {
      var j = ollama.extrair(Files.readAllBytes(documentos.caminho(id)), d.getTipoArquivo());
      return dto(persistencia.reprocessar(id, j), id);
    } catch (RuntimeException | IOException e) {
      persistencia.estado(id, "ERRO");
      throw e;
    }
  }

  public OCRResponseDTO buscarResultado(Long id) {
    return dto(
        resultados
            .findByDocumentoId(id)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extração não encontrada")),
        id);
  }

  private OCRResponseDTO dto(ResultadoOCREntity r, Long id) {
    return new OCRResponseDTO(
        r.getId(), id, r.getResultadoJson(), r.getStatus(), r.getDataProcessamento());
  }
}
