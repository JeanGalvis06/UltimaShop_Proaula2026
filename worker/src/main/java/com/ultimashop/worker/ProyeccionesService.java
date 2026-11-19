package com.ultimashop.worker;

import java.math.BigDecimal;
import java.time.*;
import java.util.UUID;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProyeccionesService {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    public ProyeccionesService(JdbcTemplate jdbc,ObjectMapper mapper) {this.jdbc=jdbc;this.mapper=mapper;}
    @Transactional(rollbackFor=Exception.class)
    public void procesar(String consumidor,String payload) throws Exception {
        if(!consumidor.equals("comprobantes") && !consumidor.equals("dashboard")) throw new IllegalArgumentException("Consumidor desconocido");
        JsonNode evento=mapper.readTree(payload);
        UUID eventId=UUID.fromString(evento.path("eventId").asText());
        long ventaId=evento.path("ventaId").asLong();
        if(!"VentaRegistrada".equals(evento.path("eventType").asText()) || evento.path("schemaVersion").asInt()!=1 || ventaId<=0)
            throw new IllegalArgumentException("Contrato de evento invalido");
        String canonico=jdbc.queryForObject("SELECT payload::text FROM outbox_eventos WHERE event_id=? AND aggregate_id=?",String.class,eventId,ventaId);
        if(!evento.equals(mapper.readTree(canonico))) throw new IllegalArgumentException("Evento no coincide con su origen");
        int nuevo=jdbc.update("INSERT INTO eventos_procesados(consumidor,event_id) VALUES (?,?) ON CONFLICT DO NOTHING",consumidor,eventId);
        if(nuevo==0) return;
        var venta=jdbc.queryForMap("SELECT id,fecha,total,total_productos,total_servicios,moneda FROM ventas WHERE id=?",ventaId);
        if(consumidor.equals("comprobantes")) {
            var detalles=jdbc.queryForList("SELECT tipo,nombre,codigo,cantidad,precio_unitario,subtotal FROM venta_detalles WHERE venta_id=? ORDER BY id",ventaId);
            jdbc.update("INSERT INTO comprobantes(venta_id,event_id,numero,total,moneda,detalle) VALUES (?,?,?,?,?,?::jsonb) ON CONFLICT (venta_id) DO NOTHING",
                ventaId,eventId,"US-"+String.format("%08d",ventaId),venta.get("total"),venta.get("moneda"),mapper.writeValueAsString(detalles));
        } else {
            LocalDate dia=((java.sql.Timestamp)venta.get("fecha")).toInstant().atZone(ZoneId.of("America/Bogota")).toLocalDate();
            int primera=jdbc.update("INSERT INTO ventas_proyectadas(venta_id,event_id,dia) VALUES (?,?,?) ON CONFLICT (venta_id) DO NOTHING",ventaId,eventId,dia);
            if(primera==1) jdbc.update("INSERT INTO resumen_ventas_diario(dia,ventas,total,total_productos,total_servicios,actualizado_en) VALUES (?,1,?,?,?,now()) ON CONFLICT (dia) DO UPDATE SET ventas=resumen_ventas_diario.ventas+1,total=resumen_ventas_diario.total+EXCLUDED.total,total_productos=resumen_ventas_diario.total_productos+EXCLUDED.total_productos,total_servicios=resumen_ventas_diario.total_servicios+EXCLUDED.total_servicios,actualizado_en=now()",
                dia,venta.get("total"),venta.get("total_productos"),venta.get("total_servicios"));
        }
    }
}
