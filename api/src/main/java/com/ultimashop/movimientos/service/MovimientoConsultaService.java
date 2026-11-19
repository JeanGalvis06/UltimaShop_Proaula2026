package com.ultimashop.movimientos.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ultimashop.comun.ZonaHoraria;
import com.ultimashop.inventario.domain.MovimientoInventario;
import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.inventario.dto.MovimientoResponse;
import com.ultimashop.inventario.repository.MovimientoRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class MovimientoConsultaService {

    private final MovimientoRepository movimientos;

    public MovimientoConsultaService(MovimientoRepository movimientos) {
        this.movimientos = movimientos;
    }

    @Transactional(readOnly = true)
    public Page<MovimientoResponse> consultar(Long productoId, TipoMovimiento tipo,
                                              LocalDate desde, LocalDate hasta,
                                              int pagina, int tamanio) {
        int numeroPagina = Math.max(pagina, 0);
        int tamanioPagina = Math.min(Math.max(tamanio, 1), 100);

        Specification<MovimientoInventario> filtros = (root, consulta, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (productoId != null) {
                condiciones.add(builder.equal(root.get("producto").get("id"), productoId));
            }
            if (tipo != null) {
                condiciones.add(builder.equal(root.get("tipo"), tipo));
            }
            if (desde != null) {
                condiciones.add(builder.greaterThanOrEqualTo(
                        root.get("fecha"), ZonaHoraria.inicioDelDia(desde)));
            }
            if (hasta != null) {
                condiciones.add(builder.lessThan(
                        root.get("fecha"), ZonaHoraria.finExclusivoDelDia(hasta)));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };

        return movimientos.findAll(filtros,
                        PageRequest.of(numeroPagina, tamanioPagina, Sort.by(Sort.Direction.DESC, "id")))
                .map(MovimientoResponse::desde);
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> ultimos(int cantidad) {
        return movimientos.findAll(PageRequest.of(0, Math.max(cantidad, 1),
                        Sort.by(Sort.Direction.DESC, "id")))
                .stream().map(MovimientoResponse::desde).toList();
    }
}
