package com.tg.Service;

import com.tg.Entities.*;
import com.tg.Repositories.*;
import java.security.*;
import java.time.Instant;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AutenticacaoService {
  private final UsuarioRepository usuarios;
  private final SessaoRepository sessoes;
  private final PasswordEncoder encoder;
  private final AuditoriaService auditoria;

  public record LoginResultado(
      String token, Instant expiraEm, Long id, String nome, String perfil) {}

  public static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  @Transactional
  public LoginResultado login(String email, String senha) {
    var u =
        usuarios
            .findByEmail(email.toLowerCase(Locale.ROOT).trim())
            .filter(UsuarioEntity::isAtivo)
            .orElseThrow(
                () ->
                    new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas"));
    if (!encoder.matches(senha, u.getSenhaHash()))
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciais inválidas");
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    SessaoEntity s = new SessaoEntity();
    s.setTokenHash(hash(token));
    s.setUsuario(u);
    s.setExpiraEm(Instant.now().plusSeconds(28800));
    sessoes.save(s);
    auditoria.registrar("ACESSO", "LOGIN", u.getId().toString());
    return new LoginResultado(token, s.getExpiraEm(), u.getId(), u.getNome(), u.getPerfil());
  }

  @Transactional
  public void logout(String token) {
    sessoes.deleteByTokenHash(hash(token));
    auditoria.registrar("ACESSO", "LOGOUT", null);
  }

  @Transactional
  public void senha(String email, String atual, String nova) {
    var u = usuarios.findByEmail(email).orElseThrow();
    if (!encoder.matches(atual, u.getSenhaHash()))
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Senha atual incorreta");
    if (nova == null || nova.length() < 12 || nova.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > 72) throw new IllegalArgumentException("Senha inválida");
    u.setSenhaHash(encoder.encode(nova));
    sessoes.deleteByUsuarioId(u.getId());
    auditoria.registrar("ACESSO", "ALTERAR_SENHA", u.getId().toString());
  }
}
