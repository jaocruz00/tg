package com.tg.Service;

import com.tg.Entities.ClienteEntity;
import com.tg.Entities.DocumentoEntity;
import com.tg.Repositories.ClienteRepository;
import com.tg.Repositories.DocumentoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentoService {

    private final DocumentoRepository documentoRepository;
    private final ClienteRepository clienteRepository;

    private final Path diretorioUpload =
            Paths.get("C:\\Users\\joaov\\OneDrive\\Documentos\\Apagar depois\\teste");

    private static final List<String> TIPOS_PERMITIDOS = List.of(
            "image/jpeg",
            "image/png",
            "application/pdf"
    );

   private static final Long TAMANHO_MAXIMO = 10485000L; // 10 mega

    public DocumentoEntity salvar(Long clienteId, MultipartFile arquivo)
            throws IOException {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "O arquivo não pode estar vazio."
            );
        }

        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new IllegalArgumentException(
                    "O arquivo não pode ultrapassar 10 MB."
            );
        }

        String tipoArquivo = arquivo.getContentType();

        if (tipoArquivo == null
                || !TIPOS_PERMITIDOS.contains(tipoArquivo)) {

            throw new IllegalArgumentException(
                    "Tipo de arquivo não permitido. "
                            + "Envie JPG, JPEG, PNG ou PDF."
            );
        }

        ClienteEntity cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Cliente não encontrado."
                        )
                );

        Files.createDirectories(diretorioUpload);

        String nomeOriginal = arquivo.getOriginalFilename();

        String extensao = obterExtensao(nomeOriginal);

        String nomeArmazenado =
                UUID.randomUUID() + extensao;

        Path caminhoCompleto =
                diretorioUpload.resolve(nomeArmazenado);

        Files.copy(
                arquivo.getInputStream(),
                caminhoCompleto
        );

        DocumentoEntity documento = new DocumentoEntity();

        documento.setNomeOriginal(nomeOriginal);
        documento.setNomeArmazenado(nomeArmazenado);
        documento.setCaminhoArquivo(
                caminhoCompleto.toAbsolutePath().toString()
        );
        documento.setTipoArquivo(tipoArquivo);
        documento.setTamanhoArquivo(arquivo.getSize());
        documento.setDataUpload(LocalDateTime.now());
        documento.setCliente(cliente);
        System.out.println("nomeOriginal: " + nomeOriginal);
        System.out.println("nomeArmazenado: " + nomeArmazenado );
        System.out.println("caminhoArquivo: " + caminhoCompleto.toAbsolutePath().toString());
        System.out.println("tamanhoArquivo: " + arquivo.getSize());
        System.out.println("tamanhoOriginal: " + arquivo.getSize());
        System.out.println("tamanhoArmazenado: " + arquivo.getSize());
        System.out.println();
        return documentoRepository.save(documento);
    }

    public  DocumentoEntity listarPorCliente(Long clienteId) {

        if (!clienteRepository.existsById(clienteId)) {
            throw new IllegalArgumentException(
                    "Cliente não encontrado."
            );
        }

        return documentoRepository.findByClienteId(clienteId);
    }

    public DocumentoEntity buscarPorId(Long id) {

        return documentoRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Documento não encontrado."
                        )
                );
    }

    public void deletar(Long id) throws IOException {

        DocumentoEntity documento = buscarPorId(id);

        Path caminhoArquivo =
                Paths.get(documento.getCaminhoArquivo());

        Files.deleteIfExists(caminhoArquivo);

        documentoRepository.deleteById(id);
    }

    private String obterExtensao(String nomeArquivo) {

        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            return "";
        }

        return nomeArquivo.substring(
                nomeArquivo.lastIndexOf(".")
        ).toLowerCase();
    }
}