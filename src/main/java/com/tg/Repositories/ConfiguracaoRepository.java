package com.tg.Repositories;

import com.tg.Entities.ConfiguracaoEntity;
import org.springframework.data.jpa.repository.*;

public interface ConfiguracaoRepository
    extends JpaRepository<ConfiguracaoEntity, Long>, JpaSpecificationExecutor<ConfiguracaoEntity> {
  java.util.Optional<ConfiguracaoEntity> findByChave(String chave);
}
