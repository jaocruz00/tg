package com.tg;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.sun.net.httpserver.HttpServer;
import com.tg.Service.*;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class OllamaClientTest {
  @Test
  void enviaPdfComoImagemELeEnvelopeNativo() throws Exception {
    var mapper = new ObjectMapper();
    var corpo = new AtomicReference<String>();
    var accessId = new AtomicReference<String>();
    var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
    server.createContext(
        "/api/generate",
        exchange -> {
          corpo.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
          accessId.set(exchange.getRequestHeaders().getFirst("CF-Access-Client-Id"));
          byte[] resposta =
              "{\"response\":\"{\\\"nome\\\":\\\"Exemplo\\\"}\"}".getBytes(StandardCharsets.UTF_8);
          exchange.getResponseHeaders().add("Content-Type", "application/json");
          exchange.sendResponseHeaders(200, resposta.length);
          exchange.getResponseBody().write(resposta);
          exchange.close();
        });
    server.start();
    try {
      var config = mock(ConfiguracaoService.class);
      when(config.modelo()).thenReturn("qwen2.5vl:latest");
      var client =
          new OllamaClient(
              config,
              "http://127.0.0.1:" + server.getAddress().getPort(),
              5,
              "teste.access",
              "segredo-ficticio");
      var resultado =
          client.extrair(new ExportacaoService().pdf("Documento sintético"), "application/pdf");
      assertEquals("Exemplo", resultado.path("nome").asText());
      var enviado = mapper.readTree(corpo.get());
      assertEquals("json", enviado.path("format").asText());
      assertFalse(enviado.path("stream").asBoolean());
      assertEquals("2m", enviado.path("keep_alive").asText());
      assertEquals(1, enviado.path("images").size());
      assertTrue(enviado.path("images").get(0).asText().startsWith("iVBOR"));
      assertEquals("teste.access", accessId.get());
    } finally {
      server.stop(0);
    }
  }
}
