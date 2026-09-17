package com.tg.DTO;

import java.time.LocalDate;

public record ClienteDTO(

        String nome,

        LocalDate dataNascimento,

        String cpf,

        String rg,

        String orgaoExpedidor,

        String telefone,

        String email,

        String endereco,

        String numero,

        String bairro,

        String cidade,

        String estado,

        String cep

) {
}