package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "configuracoes")
@Getter
@Setter
public class ConfiguracaoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String chave;

  @Column(columnDefinition = "TEXT")
  private String valor;
}
