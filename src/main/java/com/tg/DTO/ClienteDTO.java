package com.tg.DTO;

import java.time.LocalDate;

public record ClienteDTO(

        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max=255) String nome,

        LocalDate dataNascimento,

        @jakarta.validation.constraints.Pattern(regexp="[0-9]{11}") String cpf,

        @jakarta.validation.constraints.Size(max=20) String rg,

        @jakarta.validation.constraints.Size(max=20) String orgaoExpedidor,

        @jakarta.validation.constraints.Size(max=20) String telefone,

        @jakarta.validation.constraints.Email @jakarta.validation.constraints.Size(max=150) String email,

        @jakarta.validation.constraints.Size(max=255) String endereco,

        @jakarta.validation.constraints.Size(max=10) String numero,

        @jakarta.validation.constraints.Size(max=255) String bairro,

        @jakarta.validation.constraints.Size(max=255) String cidade,

        @jakarta.validation.constraints.Pattern(regexp="[A-Z]{2}") String estado,

        @jakarta.validation.constraints.Size(max=9) String cep

) {
}