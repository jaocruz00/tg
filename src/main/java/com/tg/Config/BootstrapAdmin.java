package com.tg.Config;

import com.tg.Entities.UsuarioEntity;
import com.tg.Repositories.UsuarioRepository;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.*;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class BootstrapAdmin {
  @Bean
  ApplicationRunner criarAdmin(
      UsuarioRepository repo,
      PasswordEncoder encoder,
      @Value("${app.bootstrap.email:}") String email,
      @Value("${app.bootstrap.password:}") String senha) {
    return args -> {
      if (repo.count() == 0 && !email.isBlank()) {
        if (senha.length() < 12
            || senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72)
          throw new IllegalArgumentException(
              "ADMIN_PASSWORD deve ter pelo menos 12 caracteres e até 72 bytes");
        UsuarioEntity u = new UsuarioEntity();
        u.setNome("Administrador");
        u.setEmail(email.trim().toLowerCase(Locale.ROOT));
        u.setPerfil("ADMIN");
        u.setSenhaHash(encoder.encode(senha));
        u.setAtivo(true);
        repo.save(u);
      }
    };
  }
}
