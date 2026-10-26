package com.tg.DTO;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record ClienteDTO(
    @NotBlank @Size(max = 255) String nome,
    LocalDate dataNascimento,
    @NotBlank String cpf,
    @Size(max = 20) String rg,
    @Size(max = 20) String orgaoExpedidor,
    @Size(max = 20) String telefone,
    @Email @Size(max = 150) String email,
    String endereco,
    @Size(max = 10) String numero,
    String bairro,
    String cidade,
    @Size(max = 2) String estado,
    @Size(max = 9) String cep,
    String filiacao) {}
