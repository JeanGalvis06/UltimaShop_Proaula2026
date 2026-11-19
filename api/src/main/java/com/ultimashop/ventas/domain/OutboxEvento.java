package com.ultimashop.ventas.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "outbox_eventos")
public class OutboxEvento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true)
    private UUID eventId;

    @Column(name = "event_type", nullable = false, length = 80)
    private String eventType;

    @Column(name = "schema_version", nullable = false)
    private int schemaVersion;

    @Column(name = "exchange_name", nullable = false, length = 80)
    private String exchangeName;

    @Column(name = "routing_key", nullable = false, length = 80)
    private String routingKey;

    @Column(name = "aggregate_type", nullable = false, length = 40)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false)
    private Long aggregateId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private String payload;

    @Column(nullable = false, length = 20)
    private String estado;

    @Column(nullable = false)
    private int intentos;

    @CreationTimestamp
    @Column(name = "creado_en", nullable = false)
    private Instant creadoEn;

    @Column(name = "publicado_en")
    private Instant publicadoEn;

    @Column(name = "ultimo_error")
    private String ultimoError;

    protected OutboxEvento() {
    }

    public static OutboxEvento nuevo(UUID eventId, String eventType, int schemaVersion,
                                     String exchangeName, String routingKey, String aggregateType,
                                     Long aggregateId, String payload) {
        OutboxEvento evento = new OutboxEvento();
        evento.eventId = eventId;
        evento.eventType = eventType;
        evento.schemaVersion = schemaVersion;
        evento.exchangeName = exchangeName;
        evento.routingKey = routingKey;
        evento.aggregateType = aggregateType;
        evento.aggregateId = aggregateId;
        evento.payload = payload;
        evento.estado = "PENDIENTE";
        evento.intentos = 0;
        return evento;
    }

    public Long getId() {
        return id;
    }

    public UUID getEventId() {
        return eventId;
    }

    public String getEventType() {
        return eventType;
    }

    public int getSchemaVersion() {
        return schemaVersion;
    }

    public String getExchangeName() {
        return exchangeName;
    }

    public String getRoutingKey() {
        return routingKey;
    }

    public String getAggregateType() {
        return aggregateType;
    }

    public Long getAggregateId() {
        return aggregateId;
    }

    public String getPayload() {
        return payload;
    }

    public String getEstado() {
        return estado;
    }

    public int getIntentos() {
        return intentos;
    }

    public Instant getCreadoEn() {
        return creadoEn;
    }
}
