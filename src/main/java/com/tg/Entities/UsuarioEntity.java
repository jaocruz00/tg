package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class UsuarioEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String nome;

  @Column(nullable = false, unique = true)
  private String email;

  @com.fasterxml.jackson.annotation.JsonIgnore
  @Column(nullable = false)
  private String senhaHash;

  @Column(nullable = false)
  private String perfil;

  private String oab;
  private boolean ativo = true;
}
