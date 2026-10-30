package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "minutas")
@Getter
@Setter
public class MinutaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private ClienteEntity cliente;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private ModeloVersaoEntity modeloVersao;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String conteudo;

  private String status = "RASCUNHO";
  private String autor;
  private String revisor;
  private java.time.Instant criadoEm;
  private java.time.Instant revisadoEm;
  @Version private Long revisao;
}
