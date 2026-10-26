package com.tg.Repositories;

import com.tg.Entities.DocumentoEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentoRepository extends JpaRepository<DocumentoEntity, Long> {

  List<DocumentoEntity> findByClienteId(Long clienteId);
}
