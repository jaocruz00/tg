package com.tg.Service;


import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.Base64;
import java.util.Map;

@Service
public class DocumentoTestIA {

    private static final String OPENROUTER_URL =
            "https://openrouter.ai/api/v1/chat/completions";

    @org.springframework.beans.factory.annotation.Value("${app.openrouter.key:}")
    private String apiKey;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public DocumentoTestIA(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public JsonNode gerarCliente(MultipartFile arquivo) {

        if (arquivo == null || arquivo.isEmpty()) {
            throw new IllegalArgumentException(
                    "O arquivo não pode estar vazio."
            );
        }

        if (arquivo.getSize() > 5 * 1024 * 1024) {
            throw new IllegalArgumentException(
                    "O arquivo não pode ultrapassar 5 MB."
            );
        }

        String tipoArquivo = arquivo.getContentType();

        if (tipoArquivo == null ||
                !(tipoArquivo.equals("image/jpeg")
                        || tipoArquivo.equals("image/png"))) {

            throw new IllegalArgumentException(
                    "Para este teste, envie JPG, JPEG ou PNG."
            );
        }
        try {

            /*
             * 1. Converte a imagem para Base64
             */
            byte[] bytes = arquivo.getBytes();

            String base64 = Base64.getEncoder()
                    .encodeToString(bytes);
            /*
             * 2. Cria a Data URL
             *
             * Exemplo:
             * data:image/jpeg;base64,/9j/4AAQSkZJR...
             */
            String imagemBase64 =
                    "data:" + tipoArquivo + ";base64," + base64;

            /*
             * 3. Prompt
             */
            String prompt = """
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

            /*
             * 4. Corpo da requisição
             */
            Map<String, Object> body = Map.of(
                    "model", "qwen/qwen2.5-vl-72b-instruct",

                    "messages", new Object[]{
                            Map.of(
                                    "role", "user",

                                    "content", new Object[]{
                                            Map.of(
                                                    "type", "text",
                                                    "text", prompt
                                            ),

                                            Map.of(
                                                    "type", "image_url",
                                                    "image_url", Map.of(
                                                            "url", imagemBase64
                                                    )
                                            )
                                    }
                            )
                    }
            );

            /*
             * 5. Headers
             */
            HttpHeaders headers = new HttpHeaders();

            headers.setContentType(
                    MediaType.APPLICATION_JSON
            );

            if (apiKey.isBlank()) throw new IllegalStateException("Configure OPENROUTER_API_KEY");
            headers.setBearerAuth(apiKey);

            HttpEntity<Map<String, Object>> request =
                    new HttpEntity<>(body, headers);

            /*
             * 6. Envia para OpenRouter
             */

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            OPENROUTER_URL,
                            HttpMethod.POST,
                            request,
                            String.class
                    );


            JsonNode json = objectMapper.readTree(response.getBody());

            String conteudoIA = json
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText();

            JsonNode resultadoJSON = objectMapper.readTree(conteudoIA);

            if (resultadoJSON == null || !resultadoJSON.isObject()) throw new IllegalArgumentException("Resposta IA inválida");
            return resultadoJSON;

        } catch (Exception e) {

            throw new RuntimeException(
                    "Erro ao processar documento com OpenRouter.",
                    e
            );
        }
    }
}
