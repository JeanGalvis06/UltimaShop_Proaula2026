package com.ultimashop.ventas.service;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ultimashop.comun.Auditoria;
import com.ultimashop.comun.ProveedorAuditoria;
import com.ultimashop.comun.ZonaHoraria;
import com.ultimashop.inventario.domain.MovimientoInventario;
import com.ultimashop.inventario.domain.Producto;
import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.inventario.repository.MovimientoRepository;
import com.ultimashop.inventario.repository.ProductoRepository;
import com.ultimashop.servicios.domain.Servicio;
import com.ultimashop.servicios.repository.ServicioRepository;
import com.ultimashop.ventas.domain.OutboxEvento;
import com.ultimashop.ventas.domain.TipoItemVenta;
import com.ultimashop.ventas.domain.Venta;
import com.ultimashop.ventas.domain.VentaDetalle;
import com.ultimashop.ventas.dto.VentaItemRequest;
import com.ultimashop.ventas.dto.VentaRequest;
import com.ultimashop.ventas.dto.VentaResponse;
import com.ultimashop.ventas.repository.OutboxEventoRepository;
import com.ultimashop.ventas.repository.VentaDetalleRepository;
import com.ultimashop.ventas.repository.VentaRepository;

import jakarta.persistence.EntityManager;

@Service
public class VentaService {

    private static final String MONEDA = "COP";
    private static final String EVENT_TYPE = "VentaRegistrada";
    private static final int SCHEMA_VERSION = 1;
    private static final String EXCHANGE_NAME = "ultima.events";
    private static final String ROUTING_KEY = "venta.registrada";

    private final VentaRepository ventas;
    private final VentaDetalleRepository detalles;
    private final OutboxEventoRepository outbox;
    private final ProductoRepository productos;
    private final ServicioRepository servicios;
    private final MovimientoRepository movimientos;
    private final ProveedorAuditoria proveedorAuditoria;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    public VentaService(VentaRepository ventas, VentaDetalleRepository detalles,
                        OutboxEventoRepository outbox, ProductoRepository productos,
                        ServicioRepository servicios, MovimientoRepository movimientos,
                        ProveedorAuditoria proveedorAuditoria, ObjectMapper objectMapper,
                        EntityManager entityManager) {
        this.ventas = ventas;
        this.detalles = detalles;
        this.outbox = outbox;
        this.productos = productos;
        this.servicios = servicios;
        this.movimientos = movimientos;
        this.proveedorAuditoria = proveedorAuditoria;
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    @Transactional
    public VentaResponse confirmar(VentaRequest solicitud) {
        String clave = validarClave(solicitud == null ? null : solicitud.claveIdempotencia());
        List<ItemConsolidado> items = consolidar(solicitud == null ? null : solicitud.items());
        String hash = hashSolicitud(items);

        bloquearClave(clave);
        Optional<Venta> anterior = ventas.findByClaveIdempotencia(clave);
        if (anterior.isPresent()) {
            if (!hash.equals(anterior.get().getSolicitudHash())) {
                throw VentaException.conflicto(
                        "La clave de idempotencia ya se usó con productos, servicios o cantidades distintos.");
            }
            return responder(anterior.get());
        }

        List<ItemConsolidado> itemsProducto = items.stream()
                .filter(item -> item.tipo() == TipoItemVenta.PRODUCTO)
                .sorted(Comparator.comparing(ItemConsolidado::referenciaId))
                .toList();
        List<ItemConsolidado> itemsServicio = items.stream()
                .filter(item -> item.tipo() == TipoItemVenta.SERVICIO)
                .sorted(Comparator.comparing(ItemConsolidado::referenciaId))
                .toList();

        // Bloqueo pesimista siempre en orden ascendente de id para evitar interbloqueos.
        Map<Long, Producto> productosBloqueados = new LinkedHashMap<>();
        for (ItemConsolidado item : itemsProducto) {
            Producto producto = productos.findByIdForUpdate(item.referenciaId())
                    .orElseThrow(() -> VentaException.invalido(
                            "No existe el producto con identificador " + item.referenciaId() + "."));
            if (!producto.isActivo()) {
                throw VentaException.invalido(
                        "El producto " + producto.getCodigo() + " está inactivo y no se puede vender.");
            }
            if (producto.getStockActual() < item.cantidad()) {
                throw VentaException.conflicto("Stock insuficiente para " + producto.getCodigo()
                        + ": hay " + producto.getStockActual() + " unidades y se solicitan "
                        + item.cantidad() + ".");
            }
            productosBloqueados.put(producto.getId(), producto);
        }

        Map<Long, Servicio> serviciosValidados = new LinkedHashMap<>();
        for (ItemConsolidado item : itemsServicio) {
            Servicio servicio = servicios.findById(item.referenciaId())
                    .orElseThrow(() -> VentaException.invalido(
                            "No existe el servicio con identificador " + item.referenciaId() + "."));
            if (!servicio.isActivo()) {
                throw VentaException.invalido(
                        "El servicio " + servicio.getNombre() + " está inactivo y no se puede vender.");
            }
            serviciosValidados.put(servicio.getId(), servicio);
        }

        List<DetalleCalculado> calculados = new ArrayList<>();
        BigDecimal totalProductos = BigDecimal.ZERO;
        BigDecimal totalServicios = BigDecimal.ZERO;
        for (ItemConsolidado item : items) {
            if (item.tipo() == TipoItemVenta.PRODUCTO) {
                Producto producto = productosBloqueados.get(item.referenciaId());
                BigDecimal subtotal = importe(producto.getPrecio()).multiply(BigDecimal.valueOf(item.cantidad()));
                totalProductos = totalProductos.add(subtotal);
                calculados.add(new DetalleCalculado(item.tipo(), item.referenciaId(), producto.getNombre(),
                        producto.getCodigo(), item.cantidad(), producto.getPrecio(), subtotal));
            } else {
                Servicio servicio = serviciosValidados.get(item.referenciaId());
                BigDecimal subtotal = importe(servicio.getPrecio()).multiply(BigDecimal.valueOf(item.cantidad()));
                totalServicios = totalServicios.add(subtotal);
                calculados.add(new DetalleCalculado(item.tipo(), item.referenciaId(), servicio.getNombre(),
                        servicio.getCodigo(), item.cantidad(), servicio.getPrecio(), subtotal));
            }
        }

        Auditoria auditoria = proveedorAuditoria.obtener();
        Venta venta = ventas.saveAndFlush(
                Venta.nueva(clave, hash, totalProductos, totalServicios, auditoria));

        List<VentaDetalle> detallesGuardados = new ArrayList<>();
        for (DetalleCalculado calculado : calculados) {
            detallesGuardados.add(detalles.saveAndFlush(VentaDetalle.nuevo(
                    venta, calculado.tipo(), calculado.referenciaId(), calculado.nombre(),
                    calculado.codigo(), calculado.cantidad(), calculado.precio())));
        }

        for (ItemConsolidado item : itemsProducto) {
            Producto producto = productosBloqueados.get(item.referenciaId());
            int stockAnterior = producto.getStockActual();
            int resultante = stockAnterior - item.cantidad();
            producto.fijarStock(resultante);
            movimientos.saveAndFlush(MovimientoInventario.registrarConAuditoria(
                    producto, TipoMovimiento.SALIDA, stockAnterior, -item.cantidad(), resultante,
                    "Venta " + venta.getId() + " · " + clave, venta.getId(),
                    auditoria.usuarioId(), auditoria.nombreParaAuditoria()));
        }

        guardarOutbox(venta);
        return VentaResponse.desde(venta, detallesGuardados);
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return responder(ventas.findById(id).orElseThrow(() -> VentaException.noEncontrado(id)));
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventas.findAllByOrderByFechaDesc().stream().map(this::responder).toList();
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listarDelDia() {
        LocalDate hoy = ZonaHoraria.hoy();
        return ventas.findAllByFechaBetweenOrderByFechaDesc(
                ZonaHoraria.inicioDelDia(hoy), ZonaHoraria.finExclusivoDelDia(hoy))
                .stream().map(this::responder).toList();
    }

    private VentaResponse responder(Venta venta) {
        return VentaResponse.desde(venta, detalles.findByVentaIdOrderByIdAsc(venta.getId()));
    }

    private void guardarOutbox(Venta venta) {
        UUID eventId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("eventId", eventId.toString());
        payload.put("eventType", EVENT_TYPE);
        payload.put("schemaVersion", SCHEMA_VERSION);
        payload.put("occurredAt", Instant.now().toString());
        payload.put("ventaId", venta.getId());
        payload.put("total", venta.getTotal().toPlainString());
        payload.put("totalProductos", venta.getTotalProductos().toPlainString());
        payload.put("totalServicios", venta.getTotalServicios().toPlainString());
        payload.put("moneda", MONEDA);
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("No fue posible serializar el evento de la venta.", ex);
        }
        outbox.saveAndFlush(OutboxEvento.nuevo(eventId, EVENT_TYPE, SCHEMA_VERSION,
                EXCHANGE_NAME, ROUTING_KEY, "venta", venta.getId(), json));
    }

    private void bloquearClave(String clave) {
        entityManager.createNativeQuery(
                        "SELECT pg_advisory_xact_lock(hashtext(:clave)::bigint)")
                .setParameter("clave", clave)
                .getResultList();
    }

    private String validarClave(String texto) {
        if (texto == null || texto.isBlank()) {
            throw VentaException.invalido("La clave de idempotencia es obligatoria.");
        }
        try {
            return UUID.fromString(texto.trim()).toString().toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException ex) {
            throw VentaException.invalido("La clave de idempotencia debe ser un UUID válido.");
        }
    }

    private List<ItemConsolidado> consolidar(List<VentaItemRequest> items) {
        if (items == null || items.isEmpty()) {
            throw VentaException.invalido("Agregue al menos un producto o servicio a la venta.");
        }
        Map<String, ItemConsolidado> consolidados = new LinkedHashMap<>();
        for (VentaItemRequest item : items) {
            if (item == null) {
                throw VentaException.invalido("Hay un ítem vacío en la solicitud.");
            }
            TipoItemVenta tipo = tipo(item.tipo());
            Long referenciaId = item.referenciaId();
            if (referenciaId == null || referenciaId <= 0) {
                throw VentaException.invalido("La referencia del producto o servicio es obligatoria.");
            }
            Integer cantidad = item.cantidad();
            if (cantidad == null || cantidad <= 0) {
                throw VentaException.invalido("La cantidad debe ser un entero positivo.");
            }
            String claveItem = tipo.name() + ":" + referenciaId;
            ItemConsolidado actual = consolidados.get(claveItem);
            if (actual == null) {
                consolidados.put(claveItem, new ItemConsolidado(tipo, referenciaId, cantidad));
            } else {
                int suma;
                try {
                    suma = Math.addExact(actual.cantidad(), cantidad);
                } catch (ArithmeticException ex) {
                    throw VentaException.invalido("La cantidad consolidada supera el límite permitido.");
                }
                consolidados.put(claveItem, new ItemConsolidado(tipo, referenciaId, suma));
            }
        }
        return consolidados.values().stream()
                .sorted(Comparator.comparing((ItemConsolidado item) -> item.tipo().name())
                        .thenComparing(ItemConsolidado::referenciaId))
                .toList();
    }

    private TipoItemVenta tipo(String texto) {
        if (texto == null || texto.isBlank()) {
            throw VentaException.invalido("Indique si el ítem es producto o servicio.");
        }
        try {
            return TipoItemVenta.valueOf(texto.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw VentaException.invalido("El tipo de ítem debe ser PRODUCTO o SERVICIO.");
        }
    }

    private BigDecimal importe(BigDecimal valor) {
        return valor.setScale(2, java.math.RoundingMode.HALF_UP);
    }

    private String hashSolicitud(List<ItemConsolidado> items) {
        StringBuilder canonico = new StringBuilder("v1;");
        for (ItemConsolidado item : items) {
            canonico.append(item.tipo().name())
                    .append('|').append(item.referenciaId())
                    .append('|').append(item.cantidad())
                    .append(';');
        }
        try {
            byte[] resumen = MessageDigest.getInstance("SHA-256")
                    .digest(canonico.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hexadecimal = new StringBuilder(64);
            for (byte bytes : resumen) {
                hexadecimal.append(String.format("%02x", bytes & 0xff));
            }
            return hexadecimal.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no está disponible en este entorno.", ex);
        }
    }

    private record ItemConsolidado(TipoItemVenta tipo, Long referenciaId, int cantidad) {
    }

    private record DetalleCalculado(TipoItemVenta tipo, Long referenciaId, String nombre,
                                    String codigo, int cantidad, BigDecimal precio, BigDecimal subtotal) {
    }
}
