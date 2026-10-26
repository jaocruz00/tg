package com.tg.Repositories;

import com.tg.Entities.MinutaEntity;
import org.springframework.data.jpa.repository.*;

public interface MinutaRepository
    extends JpaRepository<MinutaEntity, Long>, JpaSpecificationExecutor<MinutaEntity> {}
