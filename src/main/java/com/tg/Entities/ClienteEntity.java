package com.tg.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(
        name = "clientes",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_cliente_cpf", columnNames = "cpf")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    private LocalDate dataNascimento;

    @Column(length = 14)
    private String cpf;

    @Column(length = 20)
    private String rg;

    @Column(length = 20)
    private String orgaoExpedidor;

    @Column(length = 20)
    private String telefone;

    @Column(length = 150)
    private String email;

    private String endereco;

    @Column(length = 10)
    private String numero;

    private String bairro;

    private String cidade;

    @Column(length = 2)
    private String estado;

    @Column(length = 9)
    private String cep;
}