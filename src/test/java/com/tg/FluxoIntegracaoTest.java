package com.tg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import com.tg.Service.OllamaClient;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import tools.jackson.databind.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FluxoIntegracaoTest {
  @LocalServerPort int port;
  @MockitoBean OllamaClient ollama;
  @Autowired ClienteRepository clientes;
  @Autowired DocumentoRepository documentos;
  @Autowired ResultadoOCRRepository resultados;
  final HttpClient http = HttpClient.newHttpClient();
  final ObjectMapper mapper = new ObjectMapper();
  String token;

  HttpResponse<String> req(String method, String path, Object data, String bearer)
      throws Exception {
    var b = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
    if (bearer != null) b.header("Authorization", "Bearer " + bearer);
    if (data != null) b.header("Content-Type", "application/json");
    b.method(
        method,
        data == null
            ? HttpRequest.BodyPublishers.noBody()
            : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(data)));
    return http.send(b.build(), HttpResponse.BodyHandlers.ofString());
  }

  JsonNode json(HttpResponse<String> r) {
    return mapper.readTree(r.body());
  }

  @BeforeEach
  void login() throws Exception {
    var r =
        req(
            "POST",
            "/api/auth/login",
            Map.of("email", "admin@example.test", "senha", "Teste-Seguro-1234"),
            null);
    assertEquals(200, r.statusCode(), r.body());
    token = json(r).path("token").asText();
  }

  @Test
  void permissoesESessao() throws Exception {
    assertEquals(401, req("GET", "/api/clientes", null, null).statusCode());
    String email = "adv" + UUID.randomUUID() + "@example.test";
    var r =
        req(
            "POST",
            "/api/admin/usuarios",
            Map.of(
                "nome",
                "Advogado Teste",
                "email",
                email,
                "perfil",
                "ADVOGADO",
                "ativo",
                true,
                "senha",
                "Senha-Teste-1234",
                "oab",
                "12345/SP"),
            token);
    assertEquals(200, r.statusCode(), r.body());
    assertFalse(r.body().contains("senhaHash"));
    String adv =
        json(req(
                "POST",
                "/api/auth/login",
                Map.of("email", email, "senha", "Senha-Teste-1234"),
                null))
            .path("token")
            .asText();
    assertEquals(403, req("GET", "/api/admin/usuarios", null, adv).statusCode());
    assertEquals(
        403,
        req(
                "POST",
                "/api/modelos",
                Map.of("codigo", "teste", "nome", "teste", "conteudo", "abc"),
                adv)
            .statusCode());
    assertEquals(200, req("POST", "/api/auth/logout", null, adv).statusCode());
    assertEquals(401, req("GET", "/api/clientes", null, adv).statusCode());
  }

  @Test
  void modelosRevisaoGeracaoExportacao() throws Exception {
    ClienteEntity c = new ClienteEntity();
    c.setNome("Cliente Sintético");
    c.setCpf("52998224725");
    c.setRevisado(false);
    c = clientes.save(c);
    String codigo = "teste-" + UUID.randomUUID();
    var input = new HashMap<String, Object>();
    input.put("codigo", codigo);
    input.put("nome", "Procuração de teste");
    input.put("conteudo", "Cliente {{cliente.nome}}, CPF {{cliente.cpf}}.");
    var r = req("POST", "/api/modelos", input, token);
    assertEquals(200, r.statusCode(), r.body());
    long id = json(r).path("id").asLong();
    long rev = json(r).path("revisao").asLong();
    r = req("POST", "/api/modelos/" + id + "/publicar", Map.of("revisao", rev), token);
    assertEquals(200, r.statusCode(), r.body());
    assertEquals(
        409,
        req("POST", "/api/minutas", Map.of("clienteId", c.getId(), "modeloCodigo", codigo), token)
            .statusCode());
    c.setRevisado(true);
    clientes.save(c);
    r = req("POST", "/api/minutas", Map.of("clienteId", c.getId(), "modeloCodigo", codigo), token);
    assertEquals(200, r.statusCode(), r.body());
    var minuta = json(r);
    long mid = minuta.path("id").asLong();
    assertTrue(minuta.path("conteudo").asText().contains("Cliente Sintético"));
    assertEquals(409, req("GET", "/api/minutas/" + mid + "/arquivo", null, token).statusCode());
    r =
        req(
            "PUT",
            "/api/minutas/" + mid,
            Map.of(
                "conteudo",
                minuta.path("conteudo").asText(),
                "revisao",
                minuta.path("revisao").asLong(),
                "finalizar",
                true),
            token);
    assertEquals(200, r.statusCode(), r.body());
    for (String ext : List.of("pdf", "docx", "txt")) {
      var b =
          HttpRequest.newBuilder(
                  URI.create(
                      "http://localhost:"
                          + port
                          + "/api/minutas/"
                          + mid
                          + "/arquivo?formato="
                          + ext))
              .header("Authorization", "Bearer " + token)
              .build();
      var a = http.send(b, HttpResponse.BodyHandlers.ofByteArray());
      assertEquals(200, a.statusCode());
      assertTrue(a.body().length > 20);
    }
    var todos = json(req("GET", "/api/admin/modelos", null, token));
    for (var modelo : todos)
      if (modelo.path("id").asLong() == id) {
        input.put("revisao", modelo.path("revisao").asLong());
      }
    input.put("conteudo", "Versão nova {{cliente.nome}}");
    assertEquals(200, req("PUT", "/api/modelos/" + id, input, token).statusCode());
    var historico = json(req("GET", "/api/modelos/" + id + "/versoes", null, token));
    assertTrue(historico.isArray());
    assertTrue(historico.get(0).path("conteudo").asText().contains("cliente.cpf"));
  }

  @Test
  void uploadCriaAtualizaENaoApagaComplementos() throws Exception {
    when(ollama.extrair(any(), anyString()))
        .thenReturn(
            mapper.readTree(
                "{\"cpf\":\"111.444.777-35\",\"nome\":\"Pessoa"
                    + " Sintética\",\"data_nascimento\":\"19/DEZ/1980\",\"filiacao\":\"Filiação"
                    + " teste\",\"registro_geral\":\"1234567\"}"));
    var out = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", out);
    var body = new ByteArrayOutputStream();
    body.write(
        "--tgteste\r\nContent-Disposition: form-data; name=\"arquivo\"; filename=\"teste.png\"\r\nContent-Type: image/png\r\n\r\n"
            .getBytes(StandardCharsets.UTF_8));
    body.write(out.toByteArray());
    body.write("\r\n--tgteste--\r\n".getBytes(StandardCharsets.UTF_8));
    var request =
        HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/api/documentos/cliente"))
            .header("Authorization", "Bearer " + token)
            .header("Content-Type", "multipart/form-data; boundary=tgteste")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
            .build();
    var r = http.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, r.statusCode(), r.body());
    var j = json(r);
    long id = j.path("id").asLong();
    assertEquals("1980-12-19", j.path("dataNascimento").asText());
    assertFalse(j.path("revisado").asBoolean());
    var c = clientes.findById(id).orElseThrow();
    c.setTelefone("14999999999");
    clientes.save(c);
    r =
        req(
            "PUT",
            "/api/clientes/" + id,
            Map.of("nome", "Nome revisado", "cpf", "11144477735", "filiacao", "Filiação revisada"),
            token);
    assertEquals(200, r.statusCode(), r.body());
    assertEquals("14999999999", json(r).path("telefone").asText());
    assertEquals("Filiação revisada", json(r).path("filiacao").asText());
    r = http.send(request, HttpResponse.BodyHandlers.ofString());
    assertEquals(200, r.statusCode(), r.body());
    assertEquals(id, json(r).path("id").asLong());
    assertEquals(2, documentos.findByClienteId(id).size());
    assertEquals("14999999999", json(r).path("telefone").asText());
    for (var d : documentos.findByClienteId(id))
      assertTrue(resultados.findByDocumentoId(d.getId()).isPresent());
  }
}
