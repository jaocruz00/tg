package com.tg.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "documentos")
@lombok.Getter
@lombok.Setter
@NoArgsConstructor
@AllArgsConstructor
public class DocumentoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nomeOriginal;

    @Column(nullable = false)
    private String nomeArmazenado;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String caminhoArquivo;

    @Column(nullable = false)
    private String tipoArquivo;

    @Column(nullable = false)
    private Long tamanhoArquivo;

    @Column(nullable = false)
    private LocalDateTime dataUpload;

    @com.fasterxml.jackson.annotation.JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cliente_id", nullable = false)
    private ClienteEntity cliente;
}