package com.tg.Repositories;

import com.tg.Entities.ResultadoOCREntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResultadoOCRRepository
        extends JpaRepository<ResultadoOCREntity, Long> {

    Optional<ResultadoOCREntity> findByDocumentoId(Long documentoId);
}