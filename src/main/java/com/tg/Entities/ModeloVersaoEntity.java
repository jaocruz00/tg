package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(
    name = "modelo_versoes",
    uniqueConstraints = @UniqueConstraint(columnNames = {"modelo_id", "numero"}))
@Getter
@Setter
public class ModeloVersaoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  private ModeloEntity modelo;

  private int numero;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String conteudo;

  private String autor;
  private java.time.Instant criadoEm;
}
