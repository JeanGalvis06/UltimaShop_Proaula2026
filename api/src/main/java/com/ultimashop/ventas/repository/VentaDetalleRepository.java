package com.ultimashop.ventas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ultimashop.ventas.domain.VentaDetalle;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {

    List<VentaDetalle> findByVentaIdOrderByIdAsc(Long ventaId);
}
