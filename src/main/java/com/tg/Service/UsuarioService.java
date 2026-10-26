package com.tg.Service;

import com.tg.Entities.UsuarioEntity;
import com.tg.Repositories.*;
import jakarta.validation.constraints.*;
import java.util.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class UsuarioService {
  private final UsuarioRepository usuarios;
  private final SessaoRepository sessoes;
  private final PasswordEncoder encoder;
  private final AuditoriaService auditoria;

  public record Entrada(
      @NotBlank @Size(max = 255) String nome,
      @Email @NotBlank String email,
      @Pattern(regexp = "ADMIN|ADVOGADO") @NotNull String perfil,
      String oab,
      @NotNull Boolean ativo,
      @Size(min = 12, max = 72) String senha) {}

  @Transactional
  public UsuarioEntity salvar(Long id, Entrada d) {
    usuarios.bloquearUsuarios();
    var u =
        id == null
            ? new UsuarioEntity()
            : usuarios
                .findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    if (id != null
        && u.getEmail().equals(AuditoriaService.ator())
        && (!d.ativo() || !u.getPerfil().equals(d.perfil())))
      throw new IllegalArgumentException("Não desative nem altere seu próprio perfil.");
    if (id != null
        && u.isAtivo()
        && u.getPerfil().equals("ADMIN")
        && (!d.ativo() || !d.perfil().equals("ADMIN"))
        && usuarios.findAll().stream()
                .filter(x -> x.isAtivo() && x.getPerfil().equals("ADMIN"))
                .count()
            <= 1) throw new IllegalArgumentException("Mantenha ao menos um administrador ativo.");
    if (id == null && d.senha() == null)
      throw new IllegalArgumentException("Informe uma senha inicial com ao menos 12 caracteres.");
    String email = d.email().trim().toLowerCase(Locale.ROOT);
    usuarios
        .findByEmail(email)
        .filter(x -> !Objects.equals(x.getId(), id))
        .ifPresent(
            x -> {
              throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
            });
    u.setNome(d.nome());
    u.setEmail(email);
    u.setPerfil(d.perfil());
    u.setOab(d.oab());
    u.setAtivo(d.ativo());
    if (d.senha() != null) u.setSenhaHash(encoder.encode(d.senha()));
    if (id != null) sessoes.deleteByUsuarioId(id);
    usuarios.saveAndFlush(u);
    auditoria.registrar("USUARIOS", "SALVAR", u.getId().toString());
    return u;
  }
}
