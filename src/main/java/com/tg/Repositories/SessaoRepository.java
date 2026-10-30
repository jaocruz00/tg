package com.tg.Repositories;

import com.tg.Entities.SessaoEntity;
import org.springframework.data.jpa.repository.*;

public interface SessaoRepository
    extends JpaRepository<SessaoEntity, Long>, JpaSpecificationExecutor<SessaoEntity> {
  @EntityGraph(attributePaths="usuario")
  java.util.Optional<SessaoEntity> findByTokenHash(String hash);

  @Modifying @Query("delete from SessaoEntity s where s.expiraEm < :agora")
  int removerExpiradas(@org.springframework.data.repository.query.Param("agora") java.time.Instant agora);
  void deleteByTokenHash(String hash);

  void deleteByUsuarioId(Long id);
}
