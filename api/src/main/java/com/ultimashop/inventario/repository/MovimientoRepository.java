package com.ultimashop.inventario.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import com.ultimashop.inventario.domain.MovimientoInventario;

public interface MovimientoRepository
        extends JpaRepository<MovimientoInventario, Long>, JpaSpecificationExecutor<MovimientoInventario> {

    List<MovimientoInventario> findByProductoIdOrderByIdDesc(Long productoId);

    long countByProductoId(Long productoId);
}
