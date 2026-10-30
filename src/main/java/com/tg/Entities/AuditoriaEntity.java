package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "auditoria")
@Getter
@Setter
public class AuditoriaEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private java.time.Instant dataHora;

  @Column(nullable = false)
  private String usuario;

  @Column(nullable = false)
  private String modulo;

  @Column(nullable = false)
  private String acao;

  private String registro;
  private String origem;
}
