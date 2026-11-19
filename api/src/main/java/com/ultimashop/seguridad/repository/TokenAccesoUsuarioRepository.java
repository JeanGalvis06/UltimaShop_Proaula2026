package com.ultimashop.seguridad.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ultimashop.seguridad.domain.PropositoToken;
import com.ultimashop.seguridad.domain.TokenAccesoUsuario;

import jakarta.persistence.LockModeType;

public interface TokenAccesoUsuarioRepository extends JpaRepository<TokenAccesoUsuario, Long> {

    @Query("select t from TokenAccesoUsuario t join fetch t.usuario where t.tokenHash = :tokenHash")
    Optional<TokenAccesoUsuario> findByTokenHash(@Param("tokenHash") String tokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from TokenAccesoUsuario t join fetch t.usuario where t.tokenHash = :tokenHash")
    Optional<TokenAccesoUsuario> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update TokenAccesoUsuario t
               set t.invalidadoEn = :ahora
             where t.usuario.id = :usuarioId
               and t.proposito = :proposito
               and t.consumidoEn is null
               and t.invalidadoEn is null
            """)
    int invalidarActivos(@Param("usuarioId") Long usuarioId,
                         @Param("proposito") PropositoToken proposito,
                         @Param("ahora") Instant ahora);
}
