package com.tg.Config;
import com.tg.Entities.UsuarioEntity;
import com.tg.Repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.*;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
@Component @RequiredArgsConstructor
public class BootstrapAdmin implements ApplicationRunner {
 private final UsuarioRepository usuarios;
 private final PasswordEncoder encoder;
 @Value("${app.bootstrap.email:}") private String email;
 @Value("${app.bootstrap.password:}") private String password;
 @Override @Transactional public void run(ApplicationArguments args) {
   if(usuarios.count()!=0 || email.isBlank() || password.isBlank()) return;
   if(password.length()<12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>72) throw new IllegalArgumentException("Senha inicial inválida");
   var u=new UsuarioEntity();u.setNome("Administrador");u.setEmail(email.trim().toLowerCase(java.util.Locale.ROOT));
   u.setPerfil("ADMIN");u.setAtivo(true);u.setSenhaHash(encoder.encode(password));usuarios.save(u);
 }
}
