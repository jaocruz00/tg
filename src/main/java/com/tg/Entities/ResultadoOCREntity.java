package com.tg.Entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "resultados_ocr")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResultadoOCREntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String resultadoJson;

    @Column(nullable = false)
    private LocalDateTime dataProcessamento;

    @Column(nullable = false)
    private String status;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "documento_id", nullable = false, unique = true)
    private DocumentoEntity documento;
}