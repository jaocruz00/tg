package com.tg.Entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sessoes", indexes=@Index(name="idx_sessoes_expira_em",columnList="expiraEm"))
@Getter
@Setter
public class SessaoEntity {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true, length = 64)
  private String tokenHash;

  @ManyToOne(fetch = FetchType.EAGER, optional = false)
  private UsuarioEntity usuario;

  @Column(nullable = false)
  private java.time.Instant expiraEm;
}
