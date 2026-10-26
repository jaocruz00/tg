package com.tg.Service;

import com.tg.Entities.AuditoriaEntity;
import com.tg.Repositories.AuditoriaRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditoriaService {
  private final AuditoriaRepository repository;

  public static String ator() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a == null ? "SISTEMA" : a.getName();
  }

  public void registrar(String modulo, String acao, String registro) {
    registrar(modulo, acao, registro, "BACKEND");
  }

  public void registrar(String modulo, String acao, String registro, String origem) {
    AuditoriaEntity e = new AuditoriaEntity();
    e.setDataHora(Instant.now());
    e.setUsuario(ator());
    e.setModulo(modulo);
    e.setAcao(acao);
    e.setRegistro(registro);
    e.setOrigem(origem);
    repository.save(e);
  }
}
