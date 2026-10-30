package com.tg.Service;
import com.tg.Entities.*;
import com.tg.Repositories.*;
import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import tools.jackson.databind.JsonNode;

@Service @RequiredArgsConstructor
public class DocumentoService {
 private final DocumentoRepository documentoRepository;
 private final ClienteRepository clienteRepository;
 private final ResultadoOCRRepository resultados;
 private final DocumentoTestIA documentoTestIA;
 private final TransactionTemplate tx;
 @Value("${app.upload-dir}") private String uploadDir;
 private Path raiz() throws IOException {
   Path p = Path.of(uploadDir).toAbsolutePath().normalize();
   Files.createDirectories(p); return p.toRealPath();
 }
 public Path caminhoSeguro(DocumentoEntity d) throws IOException {
   Path p = Path.of(d.getCaminhoArquivo()).toAbsolutePath().normalize();
   Path real = p.toRealPath();
   if (!real.startsWith(raiz()) || !Files.isRegularFile(real))
     throw new ResponseStatusException(HttpStatus.FORBIDDEN);
   return real;
 }
 private String validar(MultipartFile a, boolean imagem) throws IOException {
   if (a == null || a.isEmpty() || a.getSize() > 5 * 1024 * 1024)
     throw new IllegalArgumentException("Envie arquivo de até 5 MB");
   byte[] b;
   try (var in = a.getInputStream()) { b = in.readNBytes(8); }
   String tipo;
   if (b.length >= 8 && Arrays.equals(b, new byte[]{(byte)137,80,78,71,13,10,26,10})) tipo="image/png";
   else if(b.length >= 3 && (b[0]&255)==255 && (b[1]&255)==216 && (b[2]&255)==255) tipo="image/jpeg";
   else if(!imagem && b.length >= 5 && new String(b,0,5,java.nio.charset.StandardCharsets.US_ASCII).equals("%PDF-")) tipo="application/pdf";
   else throw new IllegalArgumentException("Conteúdo de arquivo inválido");
   if (!tipo.equals(a.getContentType())) throw new IllegalArgumentException("MIME incompatível com o arquivo");
   return tipo;
 }
 public DocumentoEntity salvar(Long clienteId, MultipartFile arquivo) throws IOException {
   String tipo = validar(arquivo, false);
   Path destino = raiz().resolve(UUID.randomUUID() + switch(tipo) {
     case "image/png" -> ".png"; case "image/jpeg" -> ".jpg"; default -> ".pdf";
   });
   try {
     try (var in = arquivo.getInputStream(); var out = Files.newOutputStream(destino, StandardOpenOption.CREATE_NEW)) {
       long total=0; byte[] buffer=new byte[8192]; int n;
       while((n=in.read(buffer))!=-1) {
         total+=n; if(total>5*1024*1024) throw new IllegalArgumentException("Arquivo excede 5 MB");
         out.write(buffer,0,n);
       }
     }
     return tx.execute(status -> {
       var c=clienteRepository.findById(clienteId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
       var d=new DocumentoEntity();
       String nome=Objects.toString(arquivo.getOriginalFilename(), "documento").replaceAll("[\\r\\n\\\\/]", "_");
       d.setNomeOriginal(nome.substring(0,Math.min(200,nome.length())));
       d.setNomeArmazenado(destino.getFileName().toString()); d.setCaminhoArquivo(destino.toString());
       d.setTipoArquivo(tipo); d.setTamanhoArquivo(arquivo.getSize()); d.setDataUpload(LocalDateTime.now()); d.setCliente(c);
       return documentoRepository.saveAndFlush(d);
     });
   } catch(IOException | RuntimeException e) { Files.deleteIfExists(destino); throw e; }
 }
 public org.springframework.data.domain.Page<DocumentoEntity> listarPorCliente(Long clienteId, int page) {
   if(!clienteRepository.existsById(clienteId)) throw new ResponseStatusException(HttpStatus.NOT_FOUND);
   return documentoRepository.findByClienteId(clienteId,org.springframework.data.domain.PageRequest.of(Math.max(0,page),50));
 }
 public DocumentoEntity buscarPorId(Long id) {
   return documentoRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
 }
 public void deletar(Long id) throws IOException {
   Path caminho=caminhoSeguro(buscarPorId(id));
   Path backup=Files.move(caminho,caminho.resolveSibling(UUID.randomUUID()+".pending-delete"));
   try { tx.executeWithoutResult(s -> { resultados.deleteByDocumentoId(id); documentoRepository.deleteById(id); documentoRepository.flush(); }); }
   catch(RuntimeException e) { Files.move(backup,caminho); throw e; }
   Files.deleteIfExists(backup);
 }
 private String campo(JsonNode d,String k) {
   JsonNode v=d.path(k); return v.isTextual() && !v.asText().isBlank() ? v.asText().trim() : null;
 }
 public ClienteEntity gerarCliente(MultipartFile arquivo) throws IOException {
   validar(arquivo,true);
   JsonNode dados=documentoTestIA.gerarCliente(arquivo);
   String nome=campo(dados,"nome"), bruto=campo(dados,"cpf");
   if(nome==null || bruto==null || nome.length()>255) throw new IllegalArgumentException("Nome e CPF obrigatórios na extração");
   String cpf=bruto.replaceAll("[^0-9]", "");
   if(cpf.length()!=11) throw new IllegalArgumentException("CPF inválido na extração");
   String nascimento=campo(dados,"data_nascimento");
   LocalDate data=null;
   if(nascimento!=null) {
     try { data=LocalDate.parse(nascimento,DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT)); }
     catch(DateTimeParseException e) { try { data=LocalDate.parse(nascimento); } catch(DateTimeParseException ex) { throw new IllegalArgumentException("Data de nascimento inválida"); } }
   }
   LocalDate dt=data;
   return tx.execute(s -> {
     // A extração nunca sobrescreve dados revisados de cliente existente.
     var existente=clienteRepository.findByCpf(cpf);
     if(existente.isPresent()) return existente.get();
     var c=new ClienteEntity(); c.setNome(nome); c.setCpf(cpf); c.setDataNascimento(dt);
     String rg=campo(dados,"registro_geral"), orgao=campo(dados,"orgao_expedidor"), filiacao=campo(dados,"filiacao");
     if((rg!=null&&rg.length()>20)||(orgao!=null&&orgao.length()>20)||(filiacao!=null&&filiacao.length()>255)) throw new IllegalArgumentException("Campos extraídos excedem limites");
     c.setRg(rg); c.setOrgaoExpedidor(orgao); c.setFiliacao(filiacao); c.setRevisado(false);
     return clienteRepository.saveAndFlush(c);
   });
 }
}
