package com.ultimashop.ventas.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ultimashop.ventas.domain.Venta;

public interface VentaRepository extends JpaRepository<Venta, Long> {

    Optional<Venta> findByClaveIdempotencia(String claveIdempotencia);

    List<Venta> findAllByOrderByFechaDesc();

    List<Venta> findAllByFechaBetweenOrderByFechaDesc(Instant desde, Instant hasta);
}
