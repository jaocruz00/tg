package com.tg.Repositories;

import com.tg.Entities.ModeloEntity;
import org.springframework.data.jpa.repository.*;

public interface ModeloRepository
    extends JpaRepository<ModeloEntity, Long>, JpaSpecificationExecutor<ModeloEntity> {
  java.util.Optional<ModeloEntity> findByCodigo(String codigo);
}
