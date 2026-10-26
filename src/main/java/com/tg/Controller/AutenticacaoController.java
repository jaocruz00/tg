package com.tg.Controller;

import com.tg.Repositories.UsuarioRepository;
import com.tg.Service.AutenticacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class AutenticacaoController {
  private final AutenticacaoService auth;
  private final UsuarioRepository usuarios;

  public record Login(@NotBlank @Email String email, @NotBlank @Size(max = 72) String senha) {}

  public record Senha(@NotBlank String atual, @NotBlank @Size(min = 12, max = 72) String nova) {}

  @GetMapping("/api/health")
  public Map<String, String> health() {
    return Map.of("status", "UP");
  }

  @PostMapping("/api/auth/login")
  public AutenticacaoService.LoginResultado login(@Valid @RequestBody Login d) {
    return auth.login(d.email(), d.senha());
  }

  @PostMapping("/api/auth/logout")
  public void logout(@RequestHeader("Authorization") String header) {
    auth.logout(header.substring(7));
  }

  @GetMapping("/api/auth/me")
  public Object me(Principal p) {
    return usuarios.findByEmail(p.getName()).orElseThrow();
  }

  @PutMapping("/api/auth/senha")
  public void senha(Principal p, @Valid @RequestBody Senha d) {
    auth.senha(p.getName(), d.atual(), d.nova());
  }
}
