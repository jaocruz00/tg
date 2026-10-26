package com.tg.Controller;

import com.tg.Entities.ClienteEntity;
import com.tg.Entities.DocumentoEntity;
import com.tg.Service.DocumentoService;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor
public class DocumentoController {

  private final DocumentoService documentoService;

  @PostMapping(value = "/cliente", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
  public ResponseEntity<ClienteEntity> enviarDocumento(
      @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
    ClienteEntity documento = documentoService.gerarCliente(arquivo);
    return ResponseEntity.ok(documento);
  }

  @GetMapping("/cliente/{clienteId}")
  public ResponseEntity<List<DocumentoEntity>> listarPorCliente(@PathVariable Long clienteId) {
    return ResponseEntity.ok(documentoService.listarPorCliente(clienteId));
  }

  @GetMapping("/{id}")
  public ResponseEntity<DocumentoEntity> buscarPorId(@PathVariable Long id) {

    return ResponseEntity.ok(documentoService.buscarPorId(id));
  }

  @GetMapping("/{id}/arquivo")
  public ResponseEntity<Resource> baixarArquivo(@PathVariable Long id) throws IOException {

    DocumentoEntity documento = documentoService.buscarPorId(id);

    Path caminho = documentoService.caminho(id);

    Resource recurso = new UrlResource(caminho.toUri());

    if (!recurso.exists() || !recurso.isReadable()) {
      return ResponseEntity.notFound().build();
    }

    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(documento.getTipoArquivo()))
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            org.springframework.http.ContentDisposition.attachment()
                .filename(documento.getNomeOriginal(), java.nio.charset.StandardCharsets.UTF_8)
                .build()
                .toString())
        .body(recurso);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deletar(@PathVariable Long id) throws IOException {

    documentoService.deletar(id);

    return ResponseEntity.noContent().build();
  }
}
