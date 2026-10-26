package com.tg.Service;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ArquivoService {
  private final Path diretorio;

  public ArquivoService(@Value("${app.upload-dir}") String dir) {
    diretorio = Path.of(dir).toAbsolutePath().normalize();
  }

  public record Arquivo(byte[] bytes, String tipo, String nome) {}

  public Arquivo validar(MultipartFile file) throws IOException {
    if (file == null || file.isEmpty() || file.getSize() > 5 * 1024 * 1024)
      throw new IllegalArgumentException("Envie um arquivo não vazio de até 5 MB");
    byte[] bytes = file.getBytes();
    String tipo;
    if (bytes.length >= 5
        && new String(bytes, 0, 5, java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) {
      try (var doc = Loader.loadPDF(bytes)) {
        if (doc.isEncrypted() || doc.getNumberOfPages() < 1 || doc.getNumberOfPages() > 5)
          throw new IllegalArgumentException("PDF deve ter entre 1 e 5 páginas e não conter senha");
      }
      tipo = "application/pdf";
    } else {
      try (var in = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
        var readers = ImageIO.getImageReaders(in);
        if (!readers.hasNext()) throw new IllegalArgumentException("Envie JPG, PNG ou PDF válido");
        var reader = readers.next();
        try {
          reader.setInput(in);
          String format = reader.getFormatName().toLowerCase(Locale.ROOT);
          if (!List.of("jpeg", "jpg", "png").contains(format))
            throw new IllegalArgumentException("Formato de imagem inválido");
          if ((long) reader.getWidth(0) * reader.getHeight(0) > 20000000L)
            throw new IllegalArgumentException("Imagem deve ter até 20 megapixels");
          tipo = format.equals("png") ? "image/png" : "image/jpeg";
        } finally {
          reader.dispose();
        }
      }
    }
    String nome =
        Optional.ofNullable(file.getOriginalFilename())
            .orElse("documento")
            .replaceAll("[\\p{Cntrl}\\\\/\"]", "_");
    if (nome.length() > 200) nome = nome.substring(nome.length() - 200);
    return new Arquivo(bytes, tipo, nome);
  }

  public Path salvar(Arquivo a) throws IOException {
    Files.createDirectories(diretorio);
    Path p =
        diretorio.resolve(
            UUID.randomUUID()
                + (a.tipo().equals("application/pdf")
                    ? ".pdf"
                    : a.tipo().equals("image/png") ? ".png" : ".jpg"));
    Files.write(p, a.bytes(), StandardOpenOption.CREATE_NEW);
    return p;
  }

  public Path caminho(String valor) {
    Path p = Path.of(valor).toAbsolutePath().normalize();
    if (!p.startsWith(diretorio))
      throw new IllegalArgumentException("Arquivo fora do diretório configurado");
    return p;
  }
}
