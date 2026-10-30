package com.tg.Service;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.tg.DTO.OCRResponseDTO;
import com.tg.Entities.DocumentoEntity;
import com.tg.Entities.ResultadoOCREntity;
import com.tg.Repositories.DocumentoRepository;
import com.tg.Repositories.ResultadoOCRRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;

@Service
public class OCRService {

    private final DocumentoRepository documentoRepository;
    private final ResultadoOCRRepository resultadoOCRRepository;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;
    private final DocumentoService documentos;
    private final ConfiguracaoService config;
    private final org.springframework.transaction.support.TransactionTemplate tx;

    @Value("${app.ollama.url}")
    private String ollamaUrl;

    @Value("${app.ollama.modelo}")
    private String modelo;

    @Value("${app.ollama.cloudflare.client-id:}")
    private String cloudflareClientId;

    @Value("${app.ollama.cloudflare.client-secret:}")
    private String cloudflareClientSecret;

    public OCRService(
            DocumentoRepository documentoRepository,
            ResultadoOCRRepository resultadoOCRRepository, ObjectMapper objectMapper, RestTemplate restTemplate, DocumentoService documentos, ConfiguracaoService config, org.springframework.transaction.support.TransactionTemplate tx) {
        this.tx = tx;
        this.objectMapper = objectMapper; this.restTemplate = restTemplate; this.documentos = documentos; this.config = config;

        this.documentoRepository = documentoRepository;
        this.resultadoOCRRepository = resultadoOCRRepository;
    }

    public OCRResponseDTO processarDocumento(Long documentoId)
            throws IOException {

        DocumentoEntity documento = documentoRepository.findById(documentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Documento não encontrado."
                ));

        String tipoArquivo = documento.getTipoArquivo();

        // Nesta etapa, o OCR aceita somente JPG e PNG
        if (!"image/jpeg".equals(tipoArquivo)
                && !"image/png".equals(tipoArquivo)) {

            throw new IllegalArgumentException(
                    "Nesta etapa, o OCR aceita somente arquivos JPG e PNG."
            );
        }

        Path caminhoArquivo = documentos.caminhoSeguro(documento);

        if (!Files.exists(caminhoArquivo)) {

            throw new IllegalArgumentException(
                    "Arquivo físico não encontrado."
            );
        }

        /*
         * Cria ou recupera o resultado OCR
         * antes de iniciar o processamento.
         */
        ResultadoOCREntity resultado = tx.execute(t -> {
            documentoRepository.bloquear(documentoId).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND));
            var atual = resultadoOCRRepository.findByDocumentoId(documentoId).orElseGet(ResultadoOCREntity::new);
            if ("PROCESSANDO".equals(atual.getStatus()) && atual.getDataProcessamento().isAfter(LocalDateTime.now().minusMinutes(5)))
                throw new org.springframework.web.server.ResponseStatusException(HttpStatus.CONFLICT, "OCR em processamento");
            atual.setDocumento(documento); atual.setStatus("PROCESSANDO"); atual.setResultadoJson(null);
            atual.setDataProcessamento(LocalDateTime.now());
            return resultadoOCRRepository.saveAndFlush(atual);
        });

        try {
        if (Files.size(caminhoArquivo) > 5 * 1024 * 1024) throw new IllegalArgumentException("Arquivo excede 5 MB");
        // Lê a imagem
        byte[] arquivoBytes = Files.readAllBytes(caminhoArquivo);

        // Converte a imagem para Base64
        String imagemBase64 = Base64.getEncoder()
                .encodeToString(arquivoBytes);

        String prompt = criarPrompt();

        /*
         * Monta a requisição para o Ollama.
         */
        Map<String, Object> requestBody = Map.of(
                "model", config.modelo(),
                "prompt", prompt,
                "images", new String[]{imagemBase64},
                "stream", false,
                "format", "json"
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        if (!cloudflareClientId.isBlank() && !cloudflareClientSecret.isBlank()) {
            headers.set("CF-Access-Client-Id", cloudflareClientId);
            headers.set("CF-Access-Client-Secret", cloudflareClientSecret);
        }

        HttpEntity<Map<String, Object>> request =
                new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    ollamaUrl.replaceAll("/+$", "") + "/api/generate",
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()
                    || response.getBody() == null
                    || response.getBody().isBlank()) {

                resultado.setStatus("ERRO");
                resultado.setDataProcessamento(LocalDateTime.now());

                throw new IllegalStateException(
                        "Não foi possível obter resposta válida do Ollama."
                );
            }

            /*
             * Extrai e valida o JSON produzido pelo modelo.
             */
            String resultadoJson = extrairResposta(
                    response.getBody()
            );

            resultado.setResultadoJson(resultadoJson);
            resultado.setStatus("PROCESSADO");
            resultado.setDataProcessamento(LocalDateTime.now());

            ResultadoOCREntity resultadoSalvo =
                    resultadoOCRRepository.save(resultado);

            return converterParaDTO(resultadoSalvo);

        } catch (IOException | RuntimeException e) {

            resultado.setStatus("ERRO");
            resultado.setDataProcessamento(LocalDateTime.now());

            resultadoOCRRepository.save(resultado);

            throw new IllegalStateException(
                    "Não foi possível conectar ao servidor Ollama em "
                            + ollamaUrl,
                    e
            );
        }
    }

    public OCRResponseDTO buscarResultado(Long documentoId) {

        ResultadoOCREntity resultado = resultadoOCRRepository
                .findByDocumentoId(documentoId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Nenhum resultado OCR encontrado para este documento."
                ));

        return converterParaDTO(resultado);
    }

    private String criarPrompt() {

        return """
                Analise a imagem do documento de identificação.

                Extraia somente os dados que estiverem visíveis
                na imagem.

                Retorne somente um JSON válido.

                Não utilize Markdown.
                Não utilize blocos de código.
                Não invente informações.
                Quando um campo não estiver visível, utilize null.

                Utilize exatamente esta estrutura:

                {
                  "tipo_documento": null,
                  "registro_geral": null,
                  "data_expedicao": null,
                  "nome": null,
                  "filiacao": null,
                  "naturalidade": null,
                  "data_nascimento": null,
                  "doc_origem": null,
                  "cpf": null,
                  "orgao_expedidor": null
                }
                """;
    }

    private String extrairResposta(String respostaOllama)
            throws IOException {

        JsonNode resposta = objectMapper.readTree(respostaOllama);

        JsonNode campoResponse = resposta.get("response");

        if (campoResponse == null
                || campoResponse.isNull()
                || campoResponse.asText().isBlank()) {

            throw new IllegalStateException(
                    "O Ollama não retornou uma resposta válida."
            );
        }

        String json = campoResponse.asText();

        /*
         * Valida se o conteúdo retornado pelo modelo
         * realmente é um JSON válido.
         */
        JsonNode jsonValidado = objectMapper.readTree(json);

        if (jsonValidado == null || !jsonValidado.isObject()) throw new IllegalArgumentException("Resposta IA deve ser objeto JSON");
        return objectMapper.writeValueAsString(jsonValidado);
    }

    private OCRResponseDTO converterParaDTO(
            ResultadoOCREntity resultado) {

        return new OCRResponseDTO(
                resultado.getId(),
                resultado.getDocumento().getId(),
                resultado.getResultadoJson(),
                resultado.getStatus(),
                resultado.getDataProcessamento()
        );
    }
}
