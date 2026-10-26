package com.tg.Repositories;

import com.tg.Entities.UsuarioEntity;
import org.springframework.data.jpa.repository.*;

public interface UsuarioRepository
    extends JpaRepository<UsuarioEntity, Long>, JpaSpecificationExecutor<UsuarioEntity> {
  java.util.Optional<UsuarioEntity> findByEmail(String email);

  @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
  @org.springframework.data.jpa.repository.Query("select u from UsuarioEntity u order by u.id")
  java.util.List<UsuarioEntity> bloquearUsuarios();
}
