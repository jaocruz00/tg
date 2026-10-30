package com.tg.Controller;

import com.tg.Entities.ClienteEntity;
import com.tg.Entities.DocumentoEntity;
import com.tg.Service.DocumentoService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/documentos")
@RequiredArgsConstructor

public class DocumentoController {

    private final DocumentoService documentoService;

//    @PostMapping(
//            value = "/cliente/{clienteId}",
//            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
//    )
//    public ResponseEntity<DocumentoEntity> enviarDocumento(
//            @PathVariable Long clienteId,
//            @RequestParam("arquivo") MultipartFile arquivo
//
//    ) throws IOException {
//        System.out.println("Entrou aqui");
//        DocumentoEntity documento =
//                documentoService.salvar(clienteId, arquivo);
//
//        return ResponseEntity.ok(documento);
//    }

    @PostMapping(value = "/cliente", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClienteEntity> enviarDocumento(@RequestParam("arquivo") MultipartFile arquivo) throws IOException {
        ClienteEntity documento = documentoService.gerarCliente(arquivo);
        if (documento == null) return ResponseEntity.badRequest().build();
        return ResponseEntity.ok(documento);
    }

    @PostMapping(value="/cliente/{clienteId}", consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<DocumentoEntity> salvar(@PathVariable Long clienteId, @RequestParam("arquivo") MultipartFile arquivo) throws IOException {
      return ResponseEntity.ok(documentoService.salvar(clienteId, arquivo));
    }
    @GetMapping("/cliente/{clienteId}")
    public ResponseEntity<org.springframework.data.domain.Page<DocumentoEntity>> listarPorCliente(@PathVariable Long clienteId, @RequestParam(defaultValue="0") int page) {
        return ResponseEntity.ok(documentoService.listarPorCliente(clienteId, page));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DocumentoEntity> buscarPorId(@PathVariable Long id) {

        return ResponseEntity.ok(documentoService.buscarPorId(id));
    }

    @GetMapping("/{id}/arquivo")
    public ResponseEntity<Resource> baixarArquivo(@PathVariable Long id) throws IOException {

        DocumentoEntity documento = documentoService.buscarPorId(id);

        Path caminho = documentoService.caminhoSeguro(documento);

        Resource recurso = new UrlResource(caminho.toUri());

        if (!recurso.exists() || !recurso.isReadable()) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok().contentType(MediaType.parseMediaType(documento.getTipoArquivo())).header(HttpHeaders.CONTENT_DISPOSITION, org.springframework.http.ContentDisposition.attachment().filename(documento.getNomeOriginal(), java.nio.charset.StandardCharsets.UTF_8).build().toString()).body(recurso);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletar(@PathVariable Long id) throws IOException {

        documentoService.deletar(id);

        return ResponseEntity.noContent().build();
    }
}