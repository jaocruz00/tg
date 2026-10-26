package com.tg.Service;

import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;

@Component
public class TemplateEngine {
  public static final Set<String> CAMPOS =
      Set.of(
          "cliente.nome",
          "cliente.cpf",
          "cliente.rg",
          "cliente.filiacao",
          "cliente.dataNascimento",
          "cliente.orgaoExpedidor",
          "cliente.endereco",
          "cliente.numero",
          "cliente.bairro",
          "cliente.cidade",
          "cliente.estado",
          "cliente.cep",
          "cliente.telefone",
          "cliente.email",
          "advogado.nome",
          "advogado.oab",
          "escritorio.nome",
          "escritorio.endereco",
          "escritorio.telefone",
          "escritorio.email",
          "documento.data");
  private static final Pattern CAMPO = Pattern.compile("\\{\\{\\s*([a-zA-Z][a-zA-Z0-9.]*)\\s*}}");

  public Set<String> validar(String texto) {
    if (texto == null || texto.isBlank() || texto.length() > 100000)
      throw new IllegalArgumentException("Informe conteúdo de até 100.000 caracteres");
    Matcher m = CAMPO.matcher(texto);
    Set<String> usados = new LinkedHashSet<>();
    while (m.find()) {
      String k = m.group(1);
      if (!CAMPOS.contains(k)) throw new IllegalArgumentException("Marcador desconhecido: " + k);
      usados.add(k);
    }
    String restante = m.replaceAll("");
    if (restante.contains("{{") || restante.contains("}}"))
      throw new IllegalArgumentException("Marcador incompleto ou inválido");
    return usados;
  }

  public String preencher(String texto, Map<String, String> dados) {
    Set<String> campos = validar(texto);
    List<String> faltantes =
        campos.stream().filter(k -> dados.get(k) == null || dados.get(k).isBlank()).toList();
    if (!faltantes.isEmpty())
      throw new IllegalArgumentException("Preencha os campos: " + String.join(", ", faltantes));
    Matcher m = CAMPO.matcher(texto);
    StringBuffer out = new StringBuffer();
    while (m.find()) m.appendReplacement(out, Matcher.quoteReplacement(dados.get(m.group(1))));
    m.appendTail(out);
    return out.toString();
  }
}
