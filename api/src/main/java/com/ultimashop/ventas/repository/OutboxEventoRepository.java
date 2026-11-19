package com.ultimashop.ventas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ultimashop.ventas.domain.OutboxEvento;

public interface OutboxEventoRepository extends JpaRepository<OutboxEvento, Long> {

    List<OutboxEvento> findByAggregateIdOrderByIdAsc(Long aggregateId);

    List<OutboxEvento> findAllByEstado(String estado);

    long countByEstado(String estado);
}
