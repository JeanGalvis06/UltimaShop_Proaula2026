package com.ultimashop.servicios.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ultimashop.servicios.domain.Servicio;
import com.ultimashop.servicios.dto.ServicioRequest;
import com.ultimashop.servicios.dto.ServicioResponse;
import com.ultimashop.servicios.repository.ServicioRepository;

import jakarta.persistence.criteria.Predicate;

@Service
public class ServicioService {

    private final ServicioRepository servicios;

    public ServicioService(ServicioRepository servicios) {
        this.servicios = servicios;
    }

    @Transactional
    public ServicioResponse crear(ServicioRequest solicitud) {
        String nombre = textoObligatorio(solicitud.nombre(), 120);
        String descripcion = textoOpcional(solicitud.descripcion());
        BigDecimal precio = importe(solicitud.precio());
        Servicio servicio = servicios.saveAndFlush(Servicio.nuevo(nombre, descripcion, precio));
        return ServicioResponse.desde(servicio);
    }

    @Transactional(readOnly = true)
    public List<Servicio> listarEntidades(String q, Boolean activo) {
        Specification<Servicio> filtros = (root, consulta, builder) -> {
            List<Predicate> condiciones = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String patron = "%" + q.trim().toUpperCase(Locale.ROOT) + "%";
                condiciones.add(builder.like(builder.upper(root.get("nombre")), patron));
            }
            if (activo != null) {
                condiciones.add(builder.equal(root.get("activo"), activo));
            }
            return builder.and(condiciones.toArray(Predicate[]::new));
        };
        return servicios.findAll(filtros, Sort.by(Sort.Direction.ASC, "id"));
    }

    @Transactional(readOnly = true)
    public List<ServicioResponse> listar(String q, Boolean activo) {
        return listarEntidades(q, activo).stream().map(ServicioResponse::desde).toList();
    }

    @Transactional(readOnly = true)
    public Servicio buscarEntidad(Long id) {
        return servicios.findById(id).orElseThrow(() -> ServicioException.noEncontrado(id));
    }

    @Transactional(readOnly = true)
    public ServicioResponse obtener(Long id) {
        return ServicioResponse.desde(buscarEntidad(id));
    }

    @Transactional
    public ServicioResponse editar(Long id, ServicioRequest solicitud) {
        Servicio servicio = buscarEntidad(id);
        servicio.actualizarDatos(
                textoObligatorio(solicitud.nombre(), 120),
                textoOpcional(solicitud.descripcion()),
                importe(solicitud.precio()));
        return ServicioResponse.desde(servicio);
    }

    @Transactional
    public ServicioResponse cambiarActivo(Long id, boolean activo) {
        Servicio servicio = buscarEntidad(id);
        servicio.cambiarActivo(activo);
        return ServicioResponse.desde(servicio);
    }

    private String textoObligatorio(String valor, int longitudMaxima) {
        if (valor == null || valor.trim().isEmpty()) {
            throw ServicioException.invalido("El nombre es obligatorio.");
        }
        String texto = valor.trim();
        if (texto.length() > longitudMaxima) {
            throw ServicioException.invalido("El nombre no puede superar " + longitudMaxima + " caracteres.");
        }
        return texto;
    }

    private String textoOpcional(String valor) {
        if (valor == null || valor.trim().isEmpty()) {
            return null;
        }
        String texto = valor.trim();
        if (texto.length() > 500) {
            throw ServicioException.invalido("La descripción no puede superar 500 caracteres.");
        }
        return texto;
    }

    private BigDecimal importe(BigDecimal valor) {
        if (valor == null) {
            throw ServicioException.invalido("El precio es obligatorio.");
        }
        if (valor.signum() < 0) {
            throw ServicioException.invalido("El precio no puede ser negativo.");
        }
        BigDecimal normalizado = valor.stripTrailingZeros();
        if (normalizado.scale() > 2) {
            throw ServicioException.invalido("El precio admite máximo dos decimales.");
        }
        return normalizado.setScale(2, RoundingMode.UNNECESSARY);
    }
}
