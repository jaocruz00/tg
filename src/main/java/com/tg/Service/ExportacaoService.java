package com.tg.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;
import org.springframework.stereotype.Service;

@Service
public class ExportacaoService {
  public byte[] txt(String texto) {
    return texto.getBytes(StandardCharsets.UTF_8);
  }

  private String xml(String s) {
    return s.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "");
  }

  public byte[] docx(String texto) throws IOException {
    try (var out = new ByteArrayOutputStream();
        var zip = new ZipOutputStream(out)) {
      Map<String, String> entries = new LinkedHashMap<>();
      entries.put(
          "[Content_Types].xml",
          "<?xml version=\"1.0\"?><Types"
              + " xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\"><Default"
              + " Extension=\"rels\""
              + " ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/><Default"
              + " Extension=\"xml\" ContentType=\"application/xml\"/><Override"
              + " PartName=\"/word/document.xml\""
              + " ContentType=\"application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml\"/></Types>");
      entries.put(
          "_rels/.rels",
          "<?xml version=\"1.0\"?><Relationships"
              + " xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\"><Relationship"
              + " Id=\"rId1\""
              + " Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\""
              + " Target=\"word/document.xml\"/></Relationships>");
      StringBuilder body =
          new StringBuilder(
              "<?xml version=\"1.0\" encoding=\"UTF-8\"?><w:document"
                  + " xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>");
      for (String linha : texto.split("\\R", -1))
        body.append("<w:p><w:r><w:t xml:space=\"preserve\">")
            .append(xml(linha))
            .append("</w:t></w:r></w:p>");
      body.append(
          "<w:sectPr><w:pgSz w:w=\"11906\" w:h=\"16838\"/><w:pgMar w:top=\"1134\" w:right=\"1134\""
              + " w:bottom=\"1134\" w:left=\"1134\"/></w:sectPr></w:body></w:document>");
      entries.put("word/document.xml", body.toString());
      for (var e : entries.entrySet()) {
        zip.putNextEntry(new ZipEntry(e.getKey()));
        zip.write(e.getValue().getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
      }
      zip.finish();
      return out.toByteArray();
    }
  }

  public byte[] pdf(String texto) throws IOException {
    try (var doc = new PDDocument();
        var out = new ByteArrayOutputStream()) {
      var font = new PDType1Font(Standard14Fonts.FontName.TIMES_ROMAN);
      List<String> linhas = new ArrayList<>();
      for (String paragrafo : texto.replace("\t", "    ").split("\\R", -1)) {
        StringBuilder linha = new StringBuilder();
        for (int offset = 0; offset < paragrafo.length(); ) {
          int cp = paragrafo.codePointAt(offset);
          offset += Character.charCount(cp);
          String ch = new String(Character.toChars(cp));
          try {
            font.encode(ch);
          } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                "PDF contém caractere não suportado pela fonte padrão. Exporte DOCX ou ajuste o"
                    + " texto.");
          }
          if (font.getStringWidth(linha.toString() + ch) / 1000 * 12 > 475) {
            linhas.add(linha.toString());
            linha.setLength(0);
          }
          linha.append(ch);
        }
        linhas.add(linha.toString());
      }
      for (int i = 0; i < linhas.size(); i += 43) {
        var page = new PDPage(PDRectangle.A4);
        doc.addPage(page);
        try (var stream = new PDPageContentStream(doc, page)) {
          stream.beginText();
          stream.setFont(font, 12);
          stream.setLeading(17);
          stream.newLineAtOffset(60, 780);
          for (int n = i; n < Math.min(i + 43, linhas.size()); n++) {
            stream.showText(linhas.get(n));
            stream.newLine();
          }
          stream.endText();
        }
      }
      doc.save(out);
      return out.toByteArray();
    }
  }
}
