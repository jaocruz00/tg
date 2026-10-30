package com.tg.Repositories;

import com.tg.Entities.ModeloVersaoEntity;
import org.springframework.data.jpa.repository.*;

public interface ModeloVersaoRepository
    extends JpaRepository<ModeloVersaoEntity, Long>, JpaSpecificationExecutor<ModeloVersaoEntity> {
  java.util.Optional<ModeloVersaoEntity> findByModeloIdAndNumero(Long id, int numero);

  java.util.List<ModeloVersaoEntity> findByModeloIdOrderByNumeroDesc(Long id);
}
