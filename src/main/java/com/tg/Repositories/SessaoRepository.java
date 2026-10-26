package com.tg.Repositories;

import com.tg.Entities.SessaoEntity;
import org.springframework.data.jpa.repository.*;

public interface SessaoRepository
    extends JpaRepository<SessaoEntity, Long>, JpaSpecificationExecutor<SessaoEntity> {
  java.util.Optional<SessaoEntity> findByTokenHash(String hash);

  void deleteByTokenHash(String hash);

  void deleteByUsuarioId(Long id);
}
