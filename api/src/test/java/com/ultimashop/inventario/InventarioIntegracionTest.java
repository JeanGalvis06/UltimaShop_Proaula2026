package com.ultimashop.inventario;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.inventario.dto.AjusteRequest;
import com.ultimashop.inventario.dto.EntradaRequest;
import com.ultimashop.inventario.dto.ProductoCreateRequest;
import com.ultimashop.inventario.dto.ProductoResponse;
import com.ultimashop.inventario.dto.ProductoUpdateRequest;
import com.ultimashop.inventario.repository.MovimientoRepository;
import com.ultimashop.inventario.repository.ProductoRepository;
import com.ultimashop.inventario.service.InventarioException;
import com.ultimashop.inventario.service.InventarioService;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(username = "admin", roles = "ADMIN")
class InventarioIntegracionTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void conexionBaseDePruebas(DynamicPropertyRegistry registro) {
        registro.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registro.add("spring.datasource.username", POSTGRES::getUsername);
        registro.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private InventarioService servicio;

    @Autowired
    private ProductoRepository productos;

    @Autowired
    private MovimientoRepository movimientos;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PlatformTransactionManager transactionManager;

    /** Solo se limpia la base efímera de Testcontainers; nunca la base de desarrollo. */
    @BeforeEach
    void limpiarBaseDePruebas() {
        movimientos.deleteAll();
        productos.deleteAll();
    }

    @Test
    void crearNormalizaElCodigoYEmpiezaConStockCero() {
        Long id = crearProducto("  cafe   natural ", "Café natural", "12.50", "20.00", 2);

        ProductoResponse producto = servicio.obtener(id);
        assertEquals("CAFE NATURAL", producto.codigo());
        assertEquals("Café natural", producto.nombre());
        assertEquals(0, producto.stockActual());
        assertTrue(producto.activo());
        assertEquals(new BigDecimal("12.50"), producto.costo());

        InventarioException error = assertThrows(InventarioException.class,
                () -> crearProducto("cafe natural", "Otro café", "1", "2", 0));
        assertEquals(409, error.getStatus().value());
        assertTrue(error.getMessage().contains("Ya existe un producto con el código"));
    }

    @Test
    void rechazaImportesInvalidos() {
        assertThrows(InventarioException.class,
                () -> crearProducto("NEG", "Costo negativo", "-1", "2", 0));
        assertThrows(InventarioException.class,
                () -> crearProducto("ESC", "Tres decimales", "1.234", "2", 0));
        assertThrows(InventarioException.class,
                () -> crearProducto("PRE", "Precio negativo", "1", "-2", 0));
    }

    @Test
    void rechazaCantidadesInvalidasYMotivoVacio() {
        Long id = crearProducto("CANT", "Cantidades", "1", "2", 0);

        assertThrows(InventarioException.class, () -> crearProducto("MIN", "Mínimo", "1", "2", -1));
        assertThrows(InventarioException.class, () -> servicio.registrarEntrada(id, new EntradaRequest(0, "Compra")));
        assertThrows(InventarioException.class, () -> servicio.registrarEntrada(id, new EntradaRequest(-2, "Compra")));
        assertThrows(InventarioException.class, () -> servicio.registrarAjuste(id, new AjusteRequest(-1, "Merma")));
        assertThrows(InventarioException.class, () -> servicio.registrarEntrada(id, new EntradaRequest(1, "   ")));
    }

    @Test
    void entradaGuardaMovimientoYActualizaStock() {
        Long id = crearProducto("ENT", "Entrada", "10", "15", 5);

        var movimiento = servicio.registrarEntrada(id, new EntradaRequest(4, "Compra inicial"));

        assertEquals(TipoMovimiento.ENTRADA, movimiento.tipo());
        assertEquals(0, movimiento.cantidadAnterior());
        assertEquals(4, movimiento.cambio());
        assertEquals(4, movimiento.cantidadResultante());
        assertEquals("Compra inicial", movimiento.motivo());
        assertEquals(4, servicio.obtener(id).stockActual());
        assertEquals(1, movimientos.countByProductoId(id));
        assertEquals(1, servicio.movimientos(id).size());
    }

    @Test
    void ajusteConCantidadFinalGuardaCambioNegativo() {
        Long id = crearProducto("AJU", "Ajuste", "10", "15", 2);
        servicio.registrarEntrada(id, new EntradaRequest(6, "Compra"));

        var movimiento = servicio.registrarAjuste(id, new AjusteRequest(2, "Merma por vencimiento"));

        assertEquals(TipoMovimiento.AJUSTE, movimiento.tipo());
        assertEquals(6, movimiento.cantidadAnterior());
        assertEquals(-4, movimiento.cambio());
        assertEquals(2, movimiento.cantidadResultante());
        assertEquals("Merma por vencimiento", movimiento.motivo());
        assertEquals(2, servicio.obtener(id).stockActual());
        assertEquals(2, movimientos.countByProductoId(id));
    }

    @Test
    void productoInactivoNoAceptaMovimientos() {
        Long id = crearProducto("INA", "Inactivo", "1", "2", 0);
        servicio.cambiarActivo(id, false);

        assertThrows(InventarioException.class,
                () -> servicio.registrarEntrada(id, new EntradaRequest(1, "Compra")));
        assertThrows(InventarioException.class,
                () -> servicio.registrarAjuste(id, new AjusteRequest(0, "Merma")));
        assertEquals(0, movimientos.countByProductoId(id));
    }

    @Test
    void editarNoModificaStockNiAceptaStockEnElCuerpo() throws Exception {
        Long id = crearProducto("EDI", "Editar", "10", "15", 1);
        servicio.registrarEntrada(id, new EntradaRequest(3, "Compra"));

        servicio.editar(id, new ProductoUpdateRequest(
                "EDI-NUEVO", "Nombre nuevo", null,
                new BigDecimal("11.10"), new BigDecimal("19.90"), 7));
        assertEquals(3, servicio.obtener(id).stockActual());
        assertEquals("Nombre nuevo", servicio.obtener(id).nombre());

        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("codigo", "EDI-API");
        cuerpo.put("nombre", "Desde API");
        cuerpo.put("descripcion", null);
        cuerpo.put("costo", "11.10");
        cuerpo.put("precio", "19.90");
        cuerpo.put("stockMinimo", 7);
        cuerpo.put("stockActual", 999);
        cuerpo.put("activo", false);

        mockMvc.perform(put("/api/productos/{id}", id)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isOk());

        ProductoResponse despues = servicio.obtener(id);
        assertEquals(3, despues.stockActual());
        assertTrue(despues.activo());
        assertEquals("EDI-API", despues.codigo());
    }

    @Test
    void entradasConcurrentesNoPierdenActualizaciones() throws Exception {
        Long id = crearProducto("CONC", "Concurrencia", "1", "2", 0);
        int numeroHilos = 2;
        ExecutorService executor = Executors.newFixedThreadPool(numeroHilos);
        CountDownLatch inicio = new CountDownLatch(1);
        List<Future<?>> futuros = new ArrayList<>();

        for (int i = 0; i < numeroHilos; i++) {
            futuros.add(executor.submit(() -> {
                inicio.await();
                TransactionTemplate plantilla = new TransactionTemplate(transactionManager);
                plantilla.executeWithoutResult(estado ->
                        servicio.registrarEntrada(id, new EntradaRequest(2, "Entrada concurrente")));
                return null;
            }));
        }

        inicio.countDown();
        for (Future<?> futuro : futuros) {
            futuro.get(60, TimeUnit.SECONDS);
        }
        executor.shutdown();

        assertEquals(4, servicio.obtener(id).stockActual());
        assertEquals(2, movimientos.countByProductoId(id));
    }

    @Test
    void apiCreaListaYFiltra() throws Exception {
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("codigo", "API1");
        cuerpo.put("nombre", "Producto API");
        cuerpo.put("descripcion", "Uso de prueba");
        cuerpo.put("costo", "1.50");
        cuerpo.put("precio", "3.00");
        cuerpo.put("stockMinimo", 5);

        mockMvc.perform(post("/api/productos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("API1"))
                .andExpect(jsonPath("$.stockActual").value(0));

        mockMvc.perform(get("/api/productos")
                        .param("q", "api")
                        .param("activo", "true")
                        .param("stockBajo", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].codigo").value("API1"));
    }

    @Test
    void apiDevuelveErroresComprensiblesSinTrazas() throws Exception {
        mockMvc.perform(post("/api/productos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"codigo\":\"X\",\"nombre\":\"\",\"costo\":-1,\"precio\":1,\"stockMinimo\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.mensaje").value("Revise los campos indicados."))
                .andExpect(jsonPath("$.campos.nombre").exists())
                .andExpect(jsonPath("$..stacktrace").doesNotExist());

        mockMvc.perform(get("/api/productos/{id}", 999999))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.mensaje", containsString("No existe un producto")));

        crearProducto("DUP", "Duplicado", "1", "2", 0);
        Map<String, Object> cuerpo = new HashMap<>();
        cuerpo.put("codigo", "dup");
        cuerpo.put("nombre", "Otro");
        cuerpo.put("costo", "1");
        cuerpo.put("precio", "2");
        cuerpo.put("stockMinimo", 0);

        mockMvc.perform(post("/api/productos")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cuerpo)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensaje", containsString("Ya existe un producto")));
    }

    @Test
    void paginaInicialYPantallaDeInventarioResponden() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Dashboard")))
                .andExpect(content().string(containsString("Últimos movimientos")));

        mockMvc.perform(get("/modulos"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Inventario")))
                .andExpect(content().string(containsString("Disponible")));

        mockMvc.perform(get("/inventario"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nuevo producto")))
                .andExpect(content().string(containsString("Buscar y filtrar")));

        mockMvc.perform(get("/inventario/productos/nuevo"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Stock mínimo")))
                .andExpect(content().string(containsString("Crear producto")));
    }

    private Long crearProducto(String codigo, String nombre, String costo, String precio, int minimo) {
        return servicio.crear(new ProductoCreateRequest(
                codigo, nombre, null, new BigDecimal(costo), new BigDecimal(precio), minimo)).id();
    }
}
