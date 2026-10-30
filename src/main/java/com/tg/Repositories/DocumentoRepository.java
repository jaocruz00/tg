package com.tg.Repositories;


import com.tg.Entities.DocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository
        extends JpaRepository<DocumentoEntity, Long> {

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select d from DocumentoEntity d where d.id = :id")
    java.util.Optional<DocumentoEntity> bloquear(@org.springframework.data.repository.query.Param("id") Long id);
    org.springframework.data.domain.Page<DocumentoEntity> findByClienteId(Long clienteId, org.springframework.data.domain.Pageable pageable);
}