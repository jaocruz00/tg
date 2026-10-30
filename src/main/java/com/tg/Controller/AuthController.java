package com.tg.Controller;
import com.tg.Service.AutenticacaoService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
 private final AutenticacaoService auth;
 public record Login(@NotBlank @Email String email,@NotBlank @Size(max=72) String senha) {}
 public record Senha(@NotBlank String atual,@NotBlank @Size(min=12,max=72) String nova) {}
 @PostMapping("/login") public AutenticacaoService.LoginResultado login(@Valid @RequestBody Login d) { return auth.login(d.email(),d.senha()); }
 @PostMapping("/logout") public void logout(@RequestHeader("Authorization") String h) { auth.logout(h.substring(7)); }
 @PutMapping("/senha") public void senha(java.security.Principal p,@Valid @RequestBody Senha d) { auth.senha(p.getName(),d.atual(),d.nova()); }
}
