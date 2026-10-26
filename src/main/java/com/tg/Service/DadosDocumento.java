package com.tg.Service;

import java.time.*;
import java.time.format.*;
import java.util.*;

public final class DadosDocumento {
  private DadosDocumento() {}

  public static String cpf(String valor) {
    String n = valor == null ? "" : valor.replaceAll("[^0-9]", "");
    if (!n.matches("[0-9]{11}") || n.chars().distinct().count() == 1)
      throw new IllegalArgumentException("CPF inválido ou não identificado. Revise o documento.");
    for (int pos = 9; pos < 11; pos++) {
      int soma = 0;
      for (int i = 0; i < pos; i++) soma += (n.charAt(i) - '0') * (pos + 1 - i);
      int d = (soma * 10) % 11;
      if (d == 10) d = 0;
      if (d != n.charAt(pos) - '0')
        throw new IllegalArgumentException("CPF inválido. Revise o documento.");
    }
    return n;
  }

  public static LocalDate data(String valor) {
    if (valor == null || valor.isBlank()) return null;
    for (DateTimeFormatter f :
        List.of(
            DateTimeFormatter.ISO_LOCAL_DATE,
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT),
            new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("dd/MMM/uuuu")
                .toFormatter(Locale.forLanguageTag("pt-BR"))
                .withResolverStyle(ResolverStyle.STRICT))) {
      try {
        return LocalDate.parse(valor.trim(), f);
      } catch (DateTimeParseException ignored) {
      }
    }
    String[] meses = {
      "JAN", "FEV", "MAR", "ABR", "MAI", "JUN", "JUL", "AGO", "SET", "OUT", "NOV", "DEZ"
    };
    String texto = valor.toUpperCase(Locale.ROOT);
    for (int i = 0; i < 12; i++) texto = texto.replace(meses[i], String.format("%02d", i + 1));
    try {
      return LocalDate.parse(
          texto, DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT));
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
