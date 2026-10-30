package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "modelos")
@Getter
@Setter
public class ModeloEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String codigo;

  @Column(nullable = false)
  private String nome;

  private String categoria;
  private String area;
  private String tags;
  private String descricao;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String conteudo;

  @Column(nullable = false)
  private String status = "RASCUNHO";

  private int numeroVersao = 0;
  private String autor;
  private java.time.Instant atualizadoEm;
  @Version private Long revisao;
}
