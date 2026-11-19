package com.ultimashop.ventas;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultimashop.inventario.domain.MovimientoInventario;
import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.inventario.dto.EntradaRequest;
import com.ultimashop.inventario.dto.ProductoCreateRequest;
import com.ultimashop.inventario.repository.MovimientoRepository;
import com.ultimashop.inventario.repository.ProductoRepository;
import com.ultimashop.inventario.service.InventarioService;
import com.ultimashop.servicios.dto.ServicioRequest;
import com.ultimashop.servicios.repository.ServicioRepository;
import com.ultimashop.servicios.service.ServicioService;
import com.ultimashop.seguridad.domain.Usuario;
import com.ultimashop.seguridad.repository.UsuarioRepository;
import com.ultimashop.ventas.domain.OutboxEvento;
import com.ultimashop.ventas.dto.VentaItemRequest;
import com.ultimashop.ventas.dto.VentaRequest;
import com.ultimashop.ventas.dto.VentaResponse;
import com.ultimashop.ventas.repository.OutboxEventoRepository;
import com.ultimashop.ventas.repository.VentaDetalleRepository;
import com.ultimashop.ventas.repository.VentaRepository;
import com.ultimashop.ventas.service.VentaException;
import com.ultimashop.ventas.service.VentaService;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class ComercioIntegracionTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void conexionBaseDePruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired private VentaService ventas;
    @Autowired private InventarioService inventario;
    @Autowired private ServicioService servicios;
    @Autowired private VentaRepository ventaRepository;
    @Autowired private VentaDetalleRepository detalleRepository;
    @Autowired private OutboxEventoRepository outboxRepository;
    @Autowired private MovimientoRepository movimientoRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ServicioRepository servicioRepository;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private PasswordEncoder codificador;
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private PlatformTransactionManager transactionManager;

    @BeforeEach
    void limpiarBaseDePruebas() {
        outboxRepository.deleteAll();
        detalleRepository.deleteAll();
        movimientoRepository.deleteAll();
        ventaRepository.deleteAll();
        productoRepository.deleteAll();
        servicioRepository.deleteAll();
        usuarioRepository.deleteAll();
        usuarioRepository.saveAndFlush(Usuario.nuevo(
                "admin", codificador.encode("ClaveSegura123"), "Admin Demo", Usuario.ROL_ADMIN, true));
    }

    @Test
    void ventaMixtaConsolidaItemsDescuentaStockYGuardaOutbox() throws Exception {
        Long producto = crearProducto("MIX-01", "20.00", 10);
        Long servicio = crearServicio("Instalación", "50.00");
        String clave = UUID.randomUUID().toString();

        VentaResponse respuesta = ventas.confirmar(new VentaRequest(clave, List.of(
                item("producto", producto, 1),
                item("PRODUCTO", producto, 2),
                item("servicio", servicio, 1))));

        assertEquals(2, respuesta.items().size());
        assertEquals(new BigDecimal("110.00"), respuesta.total());
        assertEquals(new BigDecimal("60.00"), respuesta.totalProductos());
        assertEquals(new BigDecimal("50.00"), respuesta.totalServicios());
        assertEquals("COP", respuesta.moneda());
        assertEquals("Admin Demo", respuesta.usuarioNombre());
        assertEquals(7, inventario.obtener(producto).stockActual());

        List<MovimientoInventario> salidas = movimientoRepository.findByProductoIdOrderByIdDesc(producto)
                .stream().filter(movimiento -> movimiento.getTipo() == TipoMovimiento.SALIDA).toList();
        assertEquals(1, salidas.size());
        MovimientoInventario salida = salidas.get(0);
        assertEquals(TipoMovimiento.SALIDA, salida.getTipo());
        assertEquals(10, salida.getCantidadAnterior());
        assertEquals(-3, salida.getCambio());
        assertEquals(7, salida.getCantidadResultante());
        assertEquals(respuesta.id(), salida.getVentaId());
        assertEquals("Admin Demo", salida.getUsuarioNombre());

        List<OutboxEvento> eventos = outboxRepository.findByAggregateIdOrderByIdAsc(respuesta.id());
        assertEquals(1, eventos.size());
        OutboxEvento evento = eventos.get(0);
        assertEquals("VentaRegistrada", evento.getEventType());
        assertEquals(1, evento.getSchemaVersion());
        assertEquals("ultima.events", evento.getExchangeName());
        assertEquals("venta.registrada", evento.getRoutingKey());
        assertEquals("PENDIENTE", evento.getEstado());
        JsonNode payload = objectMapper.readTree(evento.getPayload());
        assertEquals("VentaRegistrada", payload.get("eventType").asText());
        assertEquals(1, payload.get("schemaVersion").asInt());
        assertEquals("110.00", payload.get("total").asText());
        assertEquals("60.00", payload.get("totalProductos").asText());
        assertEquals("50.00", payload.get("totalServicios").asText());
        assertEquals("COP", payload.get("moneda").asText());
        assertEquals(String.valueOf(respuesta.id()), payload.get("ventaId").asText());
        assertNotNull(payload.get("occurredAt").asText());
        assertTrue(payload.get("eventId").asText().matches(
                "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"));
    }

    @Test
    void faltaDeStockRevierteVentaMovimientosYOutbox() {
        Long producto = crearProducto("FALTA-01", "20.00", 2);

        VentaException error = assertThrows(VentaException.class,
                () -> ventas.confirmar(new VentaRequest(UUID.randomUUID().toString(),
                        List.of(item("PRODUCTO", producto, 5)))));

        assertEquals(409, error.getStatus().value());
        assertEquals(2, inventario.obtener(producto).stockActual());
        assertEquals(0, ventaRepository.count());
        assertEquals(0, detalleRepository.count());
        assertEquals(1, movimientoRepository.count());
        assertEquals(0L, movimientoRepository.findAll().stream()
                .filter(movimiento -> movimiento.getTipo() == TipoMovimiento.SALIDA).count());
        assertEquals(0, outboxRepository.count());
    }

    @Test
    void dobleEnvioConLaMismaClaveNoDuplicaLaVenta() {
        Long producto = crearProducto("IDEM-01", "10.00", 10);
        VentaRequest solicitud = new VentaRequest(UUID.randomUUID().toString(),
                List.of(item("PRODUCTO", producto, 2)));

        VentaResponse primera = ventas.confirmar(solicitud);
        VentaResponse segunda = ventas.confirmar(solicitud);

        assertEquals(primera.id(), segunda.id());
        assertEquals(1, ventaRepository.count());
        assertEquals(1, outboxRepository.count());
        assertEquals(1L, movimientoRepository.findAll().stream()
                .filter(movimiento -> movimiento.getTipo() == TipoMovimiento.SALIDA).count());
        assertEquals(8, inventario.obtener(producto).stockActual());

        VentaRequest distinta = new VentaRequest(solicitud.claveIdempotencia(),
                List.of(item("PRODUCTO", producto, 3)));
        VentaException error = assertThrows(VentaException.class, () -> ventas.confirmar(distinta));
        assertEquals(409, error.getStatus().value());
        assertEquals(1, ventaRepository.count());
    }

    @Test
    void concurrenciaConClavesDistintasImpideStockNegativo() throws Exception {
        Long producto = crearProducto("CONC-01", "10.00", 3);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Throwable> errores = Collections.synchronizedList(new ArrayList<>());
        List<Future<?>> futuros = new ArrayList<>();

        for (int i = 0; i < 2; i++) {
            futuros.add(executor.submit(() -> {
                inicio.await();
                TransactionTemplate plantilla = new TransactionTemplate(transactionManager);
                try {
                    plantilla.executeWithoutResult(estado -> ventas.confirmar(
                            new VentaRequest(UUID.randomUUID().toString(),
                                    List.of(item("PRODUCTO", producto, 2)))));
                } catch (Throwable error) {
                    errores.add(error);
                }
                return null;
            }));
        }
        inicio.countDown();
        for (Future<?> futuro : futuros) {
            futuro.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(1, errores.size());
        assertEquals(1, ventaRepository.count());
        assertEquals(1, outboxRepository.count());
        assertEquals(1L, movimientoRepository.findAll().stream()
                .filter(movimiento -> movimiento.getTipo() == TipoMovimiento.SALIDA).count());
        assertEquals(1, inventario.obtener(producto).stockActual());
    }

    @Test
    void productosYServiciosInactivosNoSePuedenVender() {
        Long producto = crearProducto("INA-01", "10.00", 5);
        Long servicio = crearServicio("Servicio inactivo", "20.00");
        inventario.cambiarActivo(producto, false);
        servicios.cambiarActivo(servicio, false);

        assertThrows(VentaException.class, () -> ventas.confirmar(new VentaRequest(
                UUID.randomUUID().toString(), List.of(item("PRODUCTO", producto, 1)))));
        assertThrows(VentaException.class, () -> ventas.confirmar(new VentaRequest(
                UUID.randomUUID().toString(), List.of(item("SERVICIO", servicio, 1)))));
        assertEquals(0, ventaRepository.count());
    }

    @Test
    void apiYPantallasComercialesRespondenConSeguridad() throws Exception {
        Long producto = crearProducto("API-01", "10.00", 5);
        Long servicio = crearServicio("Servicio API", "25.00");
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("claveIdempotencia", UUID.randomUUID().toString());
        cuerpo.put("items", List.of(
                Map.of("tipo", "PRODUCTO", "referenciaId", producto, "cantidad", 2),
                Map.of("tipo", "SERVICIO", "referenciaId", servicio, "cantidad", 1)));

        mockMvc.perform(post("/api/ventas")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.total").value(45.00))
                .andExpect(jsonPath("$.items.length()").value(2));

        mockMvc.perform(get("/api/ventas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].total").exists());
        mockMvc.perform(get("/api/movimientos")
                        .param("tipo", "SALIDA")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ventaId").exists());
        mockMvc.perform(get("/api/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ventasHoy").value(1))
                .andExpect(jsonPath("$.ingresosProductos").value(20.00))
                .andExpect(jsonPath("$.ingresosServicios").value(25.00));

        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ingresos de hoy")))
                .andExpect(content().string(containsString("America/Bogota")));
        mockMvc.perform(get("/ventas"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial")));
        mockMvc.perform(get("/ventas/nueva"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Total provisional")))
                .andExpect(content().string(containsString("Confirmar venta")));
        mockMvc.perform(get("/servicios"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo servicio")));
        mockMvc.perform(get("/movimientos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Historial general")));
    }

    private Long crearProducto(String codigo, String precio, int stock) {
        Long id = inventario.crear(new ProductoCreateRequest(
                codigo, "Producto " + codigo, null, new BigDecimal("1"), new BigDecimal(precio), 1)).id();
        if (stock > 0) {
            inventario.registrarEntrada(id, new EntradaRequest(stock, "Compra de prueba"));
        }
        return id;
    }

    private Long crearServicio(String nombre, String precio) {
        return servicios.crear(new ServicioRequest(
                nombre, "Servicio de prueba", new BigDecimal(precio), true)).id();
    }

    private VentaItemRequest item(String tipo, Long referenciaId, int cantidad) {
        return new VentaItemRequest(tipo, referenciaId, cantidad);
    }
}
