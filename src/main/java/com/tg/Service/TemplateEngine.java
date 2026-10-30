package com.tg.Service;
import java.util.*;
import java.util.regex.*;
import org.springframework.stereotype.Component;
@Component
public class TemplateEngine {
  private static final Pattern TOKEN = Pattern.compile("\\{\\{\\s*([a-zA-Z]+\\.[a-zA-Z]+)\\s*\\}\\}");
  private static final Set<String> CAMPOS = Set.of("cliente.nome","cliente.cpf","cliente.rg","cliente.filiacao","cliente.dataNascimento","cliente.orgaoExpedidor","cliente.endereco","cliente.numero","cliente.bairro","cliente.cidade","cliente.estado","cliente.cep","cliente.telefone","cliente.email","advogado.nome","advogado.oab","escritorio.nome","escritorio.endereco","escritorio.telefone","escritorio.email","documento.data");
  public void validar(String texto) {
    if (texto == null || texto.isBlank()) throw new IllegalArgumentException("Modelo vazio");
    Matcher m = TOKEN.matcher(texto);
    while (m.find()) if (!CAMPOS.contains(m.group(1))) throw new IllegalArgumentException("Marcador desconhecido: " + m.group(1));
    String resto = m.replaceAll("");
    if (resto.contains("{{") || resto.contains("}}")) throw new IllegalArgumentException("Marcador inválido");
  }
  public String preencher(String texto, Map<String,String> dados) {
    validar(texto);
    Matcher tokens = TOKEN.matcher(texto);
    while (tokens.find()) if (dados.get(tokens.group(1)) == null || dados.get(tokens.group(1)).isBlank())
      throw new IllegalArgumentException("Preencha o campo: " + tokens.group(1));
    return TOKEN.matcher(texto).replaceAll(m -> Matcher.quoteReplacement(dados.get(m.group(1))));
  }
}
