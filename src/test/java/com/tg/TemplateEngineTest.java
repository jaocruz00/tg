package com.tg;

import static org.junit.jupiter.api.Assertions.*;

import com.tg.Service.*;
import java.util.Map;
import org.junit.jupiter.api.Test;

class TemplateEngineTest {
  @Test
  void validaMarcadoresENaoInterpretaDados() {
    var e = new TemplateEngine();
    assertThrows(IllegalArgumentException.class, () -> e.validar("{{cliente.segredo}}"));
    assertThrows(IllegalArgumentException.class, () -> e.validar("{{cliente.nome}"));
    assertThrows(IllegalArgumentException.class, () -> e.preencher("{{cliente.nome}}", Map.of()));
    assertEquals(
        "R$ 10 \\ teste",
        e.preencher("{{cliente.nome}}", Map.of("cliente.nome", "R$ 10 \\ teste")));
  }

  @Test
  void cpfEDatas() {
    assertEquals("52998224725", DadosDocumento.cpf("529.982.247-25"));
    assertThrows(IllegalArgumentException.class, () -> DadosDocumento.cpf("11111111111"));
    assertThrows(IllegalArgumentException.class, () -> DadosDocumento.cpf("52998224726"));
    assertEquals("1980-12-19", DadosDocumento.data("19/dez/1980").toString());
    assertNull(DadosDocumento.data("31/02/2000"));
  }
}
