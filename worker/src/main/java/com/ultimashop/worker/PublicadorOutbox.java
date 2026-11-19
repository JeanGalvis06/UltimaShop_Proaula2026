package com.ultimashop.worker;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@ConditionalOnProperty(name="worker.publicador.enabled",havingValue="true",matchIfMissing=true)
public class PublicadorOutbox {
    private static final Logger LOG=LoggerFactory.getLogger(PublicadorOutbox.class);
    private final JdbcTemplate jdbc;
    private final RabbitTemplate rabbit;
    private final TransactionTemplate tx;
    public PublicadorOutbox(JdbcTemplate jdbc,RabbitTemplate rabbit,TransactionTemplate tx) {this.jdbc=jdbc;this.rabbit=rabbit;this.tx=tx;}
    @Scheduled(fixedDelayString="${worker.publicador.delay-ms:2000}")
    public void publicarPendientes() {
        try { tx.executeWithoutResult(status -> {
            var filas=jdbc.queryForList("SELECT id,event_id,payload::text,exchange_name,routing_key FROM outbox_eventos WHERE estado='PENDIENTE' AND proximo_intento_en<=now() ORDER BY id LIMIT 10 FOR UPDATE SKIP LOCKED");
            for(var fila:filas) {
                var id=fila.get("id");
                try {
                    var correlation=new CorrelationData(fila.get("event_id").toString());
                    var properties=new MessageProperties();
                    properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
                    properties.setContentEncoding("UTF-8");
                    properties.setMessageId(correlation.getId());
                    properties.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
                    rabbit.send(fila.get("exchange_name").toString(),fila.get("routing_key").toString(),
                        new Message(fila.get("payload").toString().getBytes(StandardCharsets.UTF_8),properties),correlation);
                    var confirm=correlation.getFuture().get(5,TimeUnit.SECONDS);
                    if(!confirm.isAck() || correlation.getReturned()!=null) throw new IllegalStateException("Mensaje no confirmado o no enrutable");
                    jdbc.update("UPDATE outbox_eventos SET estado='PUBLICADO',publicado_en=now(),intentos=intentos+1,ultimo_error=NULL WHERE id=?",id);
                    LOG.info("Evento publicado {}",correlation.getId());
                } catch(Exception error) {
                    if(error instanceof InterruptedException) Thread.currentThread().interrupt();
                    jdbc.update("UPDATE outbox_eventos SET intentos=intentos+1,ultimo_error='Publicacion pendiente de recuperacion',proximo_intento_en=now()+ interval '15 seconds' WHERE id=?",id);
                    LOG.warn("Outbox {} pendiente; se reintentara sin perder la venta ({})",id,error.getClass().getSimpleName());
                }
            }
        }); } catch(Exception error) { LOG.warn("No se pudo consultar outbox ({})",error.getClass().getSimpleName()); }
    }
}
