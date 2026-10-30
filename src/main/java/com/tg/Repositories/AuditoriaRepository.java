package com.tg.Repositories;

import com.tg.Entities.AuditoriaEntity;
import org.springframework.data.jpa.repository.*;

public interface AuditoriaRepository
    extends JpaRepository<AuditoriaEntity, Long>, JpaSpecificationExecutor<AuditoriaEntity> {}
