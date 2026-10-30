package com.tg;
import com.tg.Entities.*;
import com.tg.Repositories.*;
import com.tg.Service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockMultipartFile;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@SpringBootTest
class RegressionTests {
 @Autowired WebApplicationContext context;
 @Autowired UsuarioRepository usuarios;
 @Autowired SessaoRepository sessoes;
 @Autowired PasswordEncoder encoder;
 @Autowired AutenticacaoService auth;
 @Autowired DocumentoService documentos;
 @Autowired DocumentoRepository arquivos;
 @Autowired ClienteRepository clientes;
 @Autowired tools.jackson.databind.ObjectMapper mapper;
 @Autowired OCRService ocr;
 @Autowired ResultadoOCRRepository resultados;
 @Autowired org.springframework.web.client.RestTemplate rest;
 MockMvc mvc;
 @BeforeEach void setup() { mvc=MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build(); }
 @Test void anonymousIsRejected() throws Exception { mvc.perform(get("/api/clientes")).andExpect(status().isUnauthorized()); }
 @Test void lawyerCannotAdminister() throws Exception {
   mvc.perform(get("/api/admin/usuarios").with(user("adv").roles("ADVOGADO"))).andExpect(status().isForbidden());
 }
 @Test void adminHashIsNotExposed() throws Exception {
   UsuarioEntity u=new UsuarioEntity();u.setNome("Teste");u.setEmail(UUID.randomUUID()+"@teste.local");u.setPerfil("ADMIN");u.setSenhaHash(encoder.encode("senha-segura-1234"));usuarios.save(u);
   var response=mvc.perform(get("/api/admin/usuarios").with(user("admin").roles("ADMIN"))).andExpect(status().isOk()).andReturn();
   assertFalse(response.getResponse().getContentAsString().contains("senhaHash"));
 }
 @Test void bearerLoginAndRevocation() throws Exception {
   var u=new UsuarioEntity();u.setNome("Teste");u.setEmail(UUID.randomUUID()+"@teste.local");u.setPerfil("ADVOGADO");u.setSenhaHash(encoder.encode("senha-segura-1234"));usuarios.save(u);
   var result=mvc.perform(post("/api/auth/login").contentType("application/json").content(mapper.writeValueAsString(Map.of("email",u.getEmail(),"senha","senha-segura-1234"))))
     .andExpect(status().isOk()).andReturn();
   String token=mapper.readTree(result.getResponse().getContentAsString()).path("token").asText();
   mvc.perform(get("/api/clientes").header("Authorization","Bearer "+token)).andExpect(status().isOk());
   mvc.perform(get("/api/admin/painel").header("Authorization","Bearer "+token)).andExpect(status().isForbidden());
   mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isOk());
   mvc.perform(get("/api/clientes").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
 }
 @Test void invalidInputRejected() throws Exception {
   mvc.perform(post("/api/clientes").with(user("adv").roles("ADVOGADO")).contentType("application/json").content("{\"nome\":\"\",\"cpf\":\"abc\"}"))
     .andExpect(status().isBadRequest());
 }
 @Test void forgedImageRejected() {
   assertThrows(IllegalArgumentException.class,()->documentos.gerarCliente(new MockMultipartFile("arquivo","x.png","image/png","script".getBytes())));
 }
 @Test void physicalFileIsConfined() throws Exception {
   Path root=Files.createTempDirectory(Path.of("target"),"uploads-");
   ReflectionTestUtils.setField(documentos,"uploadDir",root.toString());
   Path outside=Files.createTempFile(Path.of("target"),"outside-",".png");
   try { var d=new DocumentoEntity();d.setCaminhoArquivo(outside.toAbsolutePath().toString());assertThrows(org.springframework.web.server.ResponseStatusException.class,()->documentos.caminhoSeguro(d)); }
   finally { Files.deleteIfExists(outside); }
 }
 @Test void multipleDocumentsPerClientAndSafeSerialization() throws Exception {
   Path root=Files.createTempDirectory(Path.of("target"),"uploads-");ReflectionTestUtils.setField(documentos,"uploadDir",root.toString());
   var c=new ClienteEntity();c.setNome("Cliente");c=clientes.save(c);
   byte[] png={(byte)137,80,78,71,13,10,26,10};
   var a=documentos.salvar(c.getId(),new MockMultipartFile("arquivo","a.png","image/png",png));
   var b=documentos.salvar(c.getId(),new MockMultipartFile("arquivo","b.png","image/png",png));
   assertEquals(2,documentos.listarPorCliente(c.getId(),0).getTotalElements());
   String json=mapper.writeValueAsString(a);assertFalse(json.contains("caminhoArquivo"));assertFalse(json.contains("cliente"));
   documentos.deletar(a.getId());documentos.deletar(b.getId());assertFalse(arquivos.existsById(a.getId()));
 }
 @Test void templateRejectsUnknownAndMissingDataAndKeepsLiteralReplacement() {
   var e=new TemplateEngine();assertThrows(IllegalArgumentException.class,()->e.validar("{{sistema.exec}}"));
   assertThrows(IllegalArgumentException.class,()->e.preencher("{{cliente.nome}}",Map.of()));
   assertEquals("$1",e.preencher("{{cliente.nome}}",Map.of("cliente.nome","$1")));
 }
 @Test void malformedOcrIsMarkedErrorAndRetryWorks() throws Exception {
   Path root=Files.createTempDirectory(Path.of("target"),"ocr-");ReflectionTestUtils.setField(documentos,"uploadDir",root.toString());
   var c=new ClienteEntity();c.setNome("Cliente OCR");c=clientes.save(c);
   var d=documentos.salvar(c.getId(),new MockMultipartFile("arquivo","a.png","image/png",new byte[]{(byte)137,80,78,71,13,10,26,10}));
   var server=org.springframework.test.web.client.MockRestServiceServer.bindTo(rest).build();
   try {
     server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything()).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"response\":\"invalid\"}",org.springframework.http.MediaType.APPLICATION_JSON));
     assertThrows(IllegalStateException.class,()->ocr.processarDocumento(d.getId()));
     assertEquals("ERRO",resultados.findByDocumentoId(d.getId()).orElseThrow().getStatus());server.verify();server.reset();
     server.expect(org.springframework.test.web.client.match.MockRestRequestMatchers.anything()).andRespond(org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess("{\"response\":\"{}\"}",org.springframework.http.MediaType.APPLICATION_JSON));
     assertEquals("PROCESSADO",ocr.processarDocumento(d.getId()).getStatus());server.verify();
   } finally { server.reset();documentos.deletar(d.getId()); }
 }
}
