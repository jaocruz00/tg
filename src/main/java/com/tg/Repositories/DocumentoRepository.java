package com.tg.Repositories;


import com.tg.Entities.DocumentoEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentoRepository
        extends JpaRepository<DocumentoEntity, Long> {

    DocumentoEntity findByClienteId(Long clienteId);
}