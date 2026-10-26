package com.tg.Repositories;

import com.tg.Entities.ClienteEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClienteRepository extends JpaRepository<ClienteEntity, Long> {

  Optional<ClienteEntity> findByCpf(String cpf);

  boolean existsByCpf(String cpf);
}
