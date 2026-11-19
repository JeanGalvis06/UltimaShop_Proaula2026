package com.ultimashop.inventario.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ultimashop.comun.Auditoria;
import com.ultimashop.comun.ProveedorAuditoria;
import com.ultimashop.inventario.domain.MovimientoInventario;
import com.ultimashop.inventario.domain.Producto;
import com.ultimashop.inventario.domain.TipoMovimiento;
import com.ultimashop.inventario.dto.AjusteRequest;
import com.ultimashop.inventario.dto.EntradaRequest;
import com.ultimashop.inventario.dto.MovimientoResponse;
import com.ultimashop.inventario.dto.ProductoCreateRequest;
import com.ultimashop.inventario.dto.ProductoResponse;
import com.ultimashop.inventario.dto.ProductoUpdateRequest;
import com.ultimashop.inventario.repository.MovimientoRepository;
import com.ultimashop.inventario.repository.ProductoRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class InventarioService {

    private final ProductoRepository productoRepository;
    private final MovimientoRepository movimientoRepository;
    private final ProveedorAuditoria proveedorAuditoria;

    public InventarioService(ProductoRepository productoRepository,
                             MovimientoRepository movimientoRepository,
                             ProveedorAuditoria proveedorAuditoria) {
        this.productoRepository = productoRepository;
        this.movimientoRepository = movimientoRepository;
        this.proveedorAuditoria = proveedorAuditoria;
    }

    @Transactional
    public ProductoResponse crear(ProductoCreateRequest solicitud) {
        String codigo = normalizarCodigo(solicitud.codigo());
        String nombre = textoObligatorio(solicitud.nombre(), "El nombre es obligatorio", 120);
        String descripcion = textoOpcional(solicitud.descripcion(), 500);
        BigDecimal costo = importe(solicitud.costo(), "costo");
        BigDecimal precio = importe(solicitud.precio(), "precio");
        int stockMinimo = stockMinimo(solicitud.stockMinimo());

        verificarCodigoDisponible(codigo, null);

        Producto producto;
        try {
            producto = productoRepository.saveAndFlush(
                    Producto.nuevo(codigo, nombre, descripcion, costo, precio, stockMinimo));
        } catch (DataIntegrityViolationException ex) {
            throw InventarioException.conflicto("Ya existe un producto con el código " + codigo + ".");
        }
        return ProductoResponse.desde(producto);
    }

    @Transactional(readOnly = true)
    public List<Producto> listarEntidades(String q, Boolean activo, Boolean stockBajo) {
        Specification<Producto> filtros = (root, consulta, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toUpperCase(Locale.ROOT) + "%";
                condiciones.add(builder.or(
                        builder.like(builder.upper(root.get("codigo")), patron),
                        builder.like(builder.upper(root.get("nombre")), patron)));
            }
            if (activo != null) {
                condiciones.add(builder.equal(root.get("activo"), activo));
            }
            if (stockBajo != null && stockBajo) {
                condiciones.add(builder.le(root.get("stockActual"), root.get("stockMinimo")));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
        return productoRepository.findAll(filtros, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar(String q, Boolean activo, Boolean stockBajo) {
        return listarEntidades(q, activo, stockBajo).stream()
                .map(ProductoResponse::desde)
                .toList();
    }

    @Transactional(readOnly = true)
    public Producto buscarEntidad(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> InventarioException.noEncontrado(id));
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.desde(buscarEntidad(id));
    }

    @Transactional
    public ProductoResponse editar(Long id, ProductoUpdateRequest solicitud) {
        Producto producto = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> InventarioException.noEncontrado(id));

        String codigo = normalizarCodigo(solicitud.codigo());
        verificarCodigoDisponible(codigo, id);

        producto.actualizarDatos(
                codigo,
                textoObligatorio(solicitud.nombre(), "El nombre es obligatorio", 120),
                textoOpcional(solicitud.descripcion(), 500),
                importe(solicitud.costo(), "costo"),
                importe(solicitud.precio(), "precio"),
                stockMinimo(solicitud.stockMinimo()));
        return ProductoResponse.desde(producto);
    }

    @Transactional
    public ProductoResponse cambiarActivo(Long id, boolean activo) {
        Producto producto = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> InventarioException.noEncontrado(id));
        producto.cambiarActivo(activo);
        return ProductoResponse.desde(producto);
    }

    @Transactional
    public MovimientoResponse registrarEntrada(Long id, EntradaRequest solicitud) {
        if (solicitud.cantidad() == null || solicitud.cantidad() <= 0) {
            throw InventarioException.invalido("La cantidad de la entrada debe ser mayor que cero.");
        }
        String motivo = motivo(solicitud.motivo());

        Producto producto = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> InventarioException.noEncontrado(id));
        verificarActivo(producto);

        int cantidadAnterior = producto.getStockActual();
        int cantidadResultante;
        try {
            cantidadResultante = Math.addExact(cantidadAnterior, solicitud.cantidad());
        } catch (ArithmeticException ex) {
            throw InventarioException.invalido("El stock resultante supera el límite permitido.");
        }

        producto.fijarStock(cantidadResultante);
        Auditoria auditoria = proveedorAuditoria.obtener();
        MovimientoInventario movimiento = MovimientoInventario.registrarConAuditoria(
                producto, TipoMovimiento.ENTRADA, cantidadAnterior,
                solicitud.cantidad(), cantidadResultante, motivo,
                null, auditoria.usuarioId(), auditoria.nombreParaAuditoria());
        movimientoRepository.saveAndFlush(movimiento);
        return MovimientoResponse.desde(movimiento);
    }

    @Transactional
    public MovimientoResponse registrarAjuste(Long id, AjusteRequest solicitud) {
        if (solicitud.cantidadFinal() == null || solicitud.cantidadFinal() < 0) {
            throw InventarioException.invalido("La cantidad final no puede ser negativa.");
        }
        String motivo = motivo(solicitud.motivo());

        Producto producto = productoRepository.findByIdForUpdate(id)
                .orElseThrow(() -> InventarioException.noEncontrado(id));
        verificarActivo(producto);

        int cantidadAnterior = producto.getStockActual();
        int cambio = solicitud.cantidadFinal() - cantidadAnterior;
        producto.fijarStock(solicitud.cantidadFinal());

        Auditoria auditoria = proveedorAuditoria.obtener();
        MovimientoInventario movimiento = MovimientoInventario.registrarConAuditoria(
                producto, TipoMovimiento.AJUSTE, cantidadAnterior,
                cambio, solicitud.cantidadFinal(), motivo,
                null, auditoria.usuarioId(), auditoria.nombreParaAuditoria());
        movimientoRepository.saveAndFlush(movimiento);
        return MovimientoResponse.desde(movimiento);
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> movimientos(Long id) {
        if (!productoRepository.existsById(id)) {
            throw InventarioException.noEncontrado(id);
        }
        return movimientoRepository.findByProductoIdOrderByIdDesc(id).stream()
                .map(MovimientoResponse::desde)
                .toList();
    }

    private void verificarCodigoDisponible(String codigo, Long idActual) {
        Producto existente = productoRepository.findByCodigo(codigo).orElse(null);
        if (existente != null && !existente.getId().equals(idActual)) {
            throw InventarioException.conflicto("Ya existe un producto con el código " + codigo + ".");
        }
    }

    private void verificarActivo(Producto producto) {
        if (!producto.isActivo()) {
            throw InventarioException.invalido(
                    "No se pueden registrar movimientos en el producto " + producto.getCodigo()
                            + " porque está inactivo.");
        }
    }

    private String normalizarCodigo(String codigo) {
        if (codigo == null || codigo.trim().isEmpty()) {
            throw InventarioException.invalido("El código es obligatorio.");
        }
        String normalizado = codigo.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ROOT);
        if (normalizado.length() > 40) {
            throw InventarioException.invalido("El código no puede superar 40 caracteres.");
        }
        return normalizado;
    }

    private String textoObligatorio(String valor, String mensaje, int longitudMaxima) {
        if (valor == null || valor.trim().isEmpty()) {
            throw InventarioException.invalido(mensaje);
        }
        String texto = valor.trim();
        if (texto.length() > longitudMaxima) {
            throw InventarioException.invalido("El texto no puede superar " + longitudMaxima + " caracteres.");
        }
        return texto;
    }

    private String textoOpcional(String valor, int longitudMaxima) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String texto = valor.trim();
        if (texto.length() > longitudMaxima) {
            throw InventarioException.invalido("El texto no puede superar " + longitudMaxima + " caracteres.");
        }
        return texto;
    }

    private BigDecimal importe(BigDecimal valor, String campo) {
        if (valor == null) {
            throw InventarioException.invalido("El " + campo + " es obligatorio.");
        }
        if (valor.signum() < 0) {
            throw InventarioException.invalido("El " + campo + " no puede ser negativo.");
        }
        BigDecimal normalizado = valor.stripTrailingZeros();
        if (normalizado.scale() > 2) {
            throw InventarioException.invalido("El " + campo + " admite máximo dos decimales.");
        }
        return normalizado.setScale(2, RoundingMode.UNNECESSARY);
    }

    private int stockMinimo(Integer valor) {
        if (valor == null || valor < 0) {
            throw InventarioException.invalido("El stock mínimo no puede ser negativo.");
        }
        return valor;
    }

    private String motivo(String motivo) {
        if (motivo == null || motivo.trim().isEmpty()) {
            throw InventarioException.invalido("El motivo es obligatorio.");
        }
        String texto = motivo.trim();
        if (texto.length() > 300) {
            throw InventarioException.invalido("El motivo no puede superar 300 caracteres.");
        }
        return texto;
    }
}
