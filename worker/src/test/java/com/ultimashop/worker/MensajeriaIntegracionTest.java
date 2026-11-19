package com.ultimashop.worker;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.*;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.RabbitListenerEndpointRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(properties={"worker.publicador.enabled=false","worker.consumidores.enabled=false","server.port=0"})
class MensajeriaIntegracionTest {
    @Container static PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:16-alpine");
    @Container static RabbitMQContainer broker=new RabbitMQContainer("rabbitmq:4-management");
    @DynamicPropertySource static void propiedades(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url",postgres::getJdbcUrl);r.add("spring.datasource.username",postgres::getUsername);r.add("spring.datasource.password",postgres::getPassword);
        r.add("spring.rabbitmq.host",broker::getHost);r.add("spring.rabbitmq.port",broker::getAmqpPort);
        r.add("spring.rabbitmq.username",broker::getAdminUsername);r.add("spring.rabbitmq.password",broker::getAdminPassword);
    }
    @BeforeAll static void migrar() {
        Flyway.configure().dataSource(postgres.getJdbcUrl(),postgres.getUsername(),postgres.getPassword())
            .locations("filesystem:../api/src/main/resources/db/migration").load().migrate();
    }
    @Autowired JdbcTemplate jdbc;
    @Autowired RabbitTemplate rabbit;
    @Autowired ObjectMapper mapper;
    @Autowired ProyeccionesService proyecciones;
    @Autowired PlatformTransactionManager transactionManager;
    @Autowired RabbitAdmin admin;
    @Autowired RabbitListenerEndpointRegistry listeners;
    @BeforeEach void limpiar() {
        listeners.stop();admin.initialize();
        for(String cola:List.of(RabbitConfig.COMPROBANTES,RabbitConfig.DASHBOARD,RabbitConfig.COMPROBANTES+".errores",RabbitConfig.DASHBOARD+".errores")) admin.purgeQueue(cola);
        jdbc.execute("TRUNCATE comprobantes,eventos_procesados,ventas_proyectadas,resumen_ventas_diario,outbox_eventos,ventas RESTART IDENTITY CASCADE");
    }
    private String venta() throws Exception {
        long ventaId=jdbc.queryForObject("INSERT INTO ventas(clave_idempotencia,solicitud_hash,total,total_productos,total_servicios) VALUES (?,repeat('a',64),25,20,5) RETURNING id",Long.class,UUID.randomUUID().toString());
        jdbc.update("INSERT INTO venta_detalles(venta_id,tipo,referencia_id,nombre,codigo,cantidad,precio_unitario,subtotal) VALUES (?,'PRODUCTO',1,'Producto de prueba','TEST',2,10,20)",ventaId);
        var eventId=UUID.randomUUID();
        String payload=mapper.writeValueAsString(Map.of("eventId",eventId.toString(),"eventType","VentaRegistrada","schemaVersion",1,"occurredAt",Instant.now().toString(),"ventaId",ventaId,"total","25.00","totalProductos","20.00","totalServicios","5.00","moneda","COP"));
        jdbc.update("INSERT INTO outbox_eventos(event_id,event_type,schema_version,exchange_name,routing_key,aggregate_type,aggregate_id,payload) VALUES (?,'VentaRegistrada',1,?,?,'venta',?,?::jsonb)",eventId,RabbitConfig.EXCHANGE,RabbitConfig.KEY,ventaId,payload);
        return payload;
    }
    private PublicadorOutbox publicador() {return new PublicadorOutbox(jdbc,rabbit,new TransactionTemplate(transactionManager));}
    @Test void publicaEnDosColasYProyectaSinDuplicados() throws Exception {
        String evento=venta();publicador().publicarPendientes();
        assertEquals("PUBLICADO",jdbc.queryForObject("SELECT estado FROM outbox_eventos",String.class));
        Message comprobante=rabbit.receive(RabbitConfig.COMPROBANTES,5000);
        Message resumen=rabbit.receive(RabbitConfig.DASHBOARD,5000);
        assertNotNull(comprobante);assertNotNull(resumen);
        proyecciones.procesar("comprobantes",new String(comprobante.getBody(),StandardCharsets.UTF_8));
        proyecciones.procesar("dashboard",new String(resumen.getBody(),StandardCharsets.UTF_8));
        proyecciones.procesar("comprobantes",evento);proyecciones.procesar("dashboard",evento);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM comprobantes",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT ventas FROM resumen_ventas_diario",Integer.class));
        assertEquals(0,new java.math.BigDecimal("25.00").compareTo(jdbc.queryForObject("SELECT total FROM resumen_ventas_diario",java.math.BigDecimal.class)));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM eventos_procesados",Integer.class));
    }
    @Test void dosEntregasConcurrentesNoDuplicanEfectos() throws Exception {
        String evento=venta();
        try(var pool=Executors.newFixedThreadPool(4)) {
            var tareas=new ArrayList<Future<?>>();
            for(int i=0;i<8;i++) tareas.add(pool.submit(()->{try {proyecciones.procesar("dashboard",evento);}catch(Exception e){throw new RuntimeException(e);}}));
            for(var tarea:tareas)tarea.get(20,TimeUnit.SECONDS);
        }
        assertEquals(1,jdbc.queryForObject("SELECT ventas FROM resumen_ventas_diario",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM eventos_procesados",Integer.class));
    }
    @Test void consumidorAusenteConservaYRecuperaPendientes() throws Exception {
        venta();publicador().publicarPendientes();
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM comprobantes",Integer.class));
        assertEquals(1,admin.getQueueInfo(RabbitConfig.COMPROBANTES).getMessageCount());
        listeners.start();
        try {
            long limite=System.currentTimeMillis()+15000;
            while(System.currentTimeMillis()<limite && jdbc.queryForObject("SELECT count(*) FROM eventos_procesados",Integer.class)<2) Thread.sleep(150);
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM comprobantes",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT ventas FROM resumen_ventas_diario",Integer.class));
        } finally {listeners.stop();}
    }
    @Test void mensajeInvalidoVaAColasDeErroresTrasReintentos() {
        listeners.start();
        try {
            var p=new MessageProperties();p.setContentType("application/json");
            rabbit.send(RabbitConfig.EXCHANGE,RabbitConfig.KEY,new Message("{}".getBytes(StandardCharsets.UTF_8),p));
            assertNotNull(rabbit.receive(RabbitConfig.COMPROBANTES+".errores",15000));
            assertNotNull(rabbit.receive(RabbitConfig.DASHBOARD+".errores",15000));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM eventos_procesados",Integer.class));
        } finally {listeners.stop();}
    }
    @Test void mensajeNoEnrutablePermaneceEnOutboxHastaCorregir() throws Exception {
        venta();jdbc.update("UPDATE outbox_eventos SET routing_key='sin.destino'");
        publicador().publicarPendientes();
        assertEquals("PENDIENTE",jdbc.queryForObject("SELECT estado FROM outbox_eventos",String.class));
        assertEquals(1,jdbc.queryForObject("SELECT intentos FROM outbox_eventos",Integer.class));
        jdbc.update("UPDATE outbox_eventos SET routing_key=?,proximo_intento_en=now()",RabbitConfig.KEY);
        publicador().publicarPendientes();
        assertEquals("PUBLICADO",jdbc.queryForObject("SELECT estado FROM outbox_eventos",String.class));
    }
    @Test void eventoManipuladoNoDejaMarcaNiEfecto() throws Exception {
        String evento=venta();var json=mapper.readTree(evento);((com.fasterxml.jackson.databind.node.ObjectNode)json).put("total","1.00");
        assertThrows(IllegalArgumentException.class,()->proyecciones.procesar("dashboard",mapper.writeValueAsString(json)));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM eventos_procesados",Integer.class));
    }
}
