package com.tg.Repositories;

import com.tg.Entities.ResultadoOCREntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResultadoOCRRepository extends JpaRepository<ResultadoOCREntity, Long> {

  Optional<ResultadoOCREntity> findByDocumentoId(Long documentoId);
}
