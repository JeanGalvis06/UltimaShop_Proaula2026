package com.ultimashop.seguridad.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "tokens_acceso_usuario")
public class TokenAccesoUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PropositoToken proposito;

    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "expira_en", nullable = false)
    private Instant expiraEn;

    @Column(name = "consumido_en")
    private Instant consumidoEn;

    @Column(name = "invalidado_en")
    private Instant invalidadoEn;

    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "solicitado_ip", length = 45)
    private String solicitadoIp;

    protected TokenAccesoUsuario() {
    }

    public TokenAccesoUsuario(Usuario usuario, PropositoToken proposito, String tokenHash,
                              Instant expiraEn, Instant creadoEn, String solicitadoIp) {
        this.usuario = usuario;
        this.proposito = proposito;
        this.tokenHash = tokenHash;
        this.expiraEn = expiraEn;
        this.creadoEn = creadoEn;
        this.solicitadoIp = solicitadoIp;
    }

    public boolean estaVigente(Instant ahora) {
        return consumidoEn == null && invalidadoEn == null && expiraEn.isAfter(ahora);
    }

    public void consumir(Instant ahora) {
        this.consumidoEn = ahora;
    }

    public void invalidar(Instant ahora) {
        if (consumidoEn == null && invalidadoEn == null) {
            this.invalidadoEn = ahora;
        }
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public PropositoToken getProposito() {
        return proposito;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiraEn() {
        return expiraEn;
    }

    public Instant getConsumidoEn() {
        return consumidoEn;
    }

    public Instant getInvalidadoEn() {
        return invalidadoEn;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }

    public String getSolicitadoIp() {
        return solicitadoIp;
    }
}
