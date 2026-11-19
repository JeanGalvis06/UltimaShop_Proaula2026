package com.ultimashop.comun;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Todas las fechas visibles se expresan en America/Bogota; en base se guarda UTC. */
public final class ZonaHoraria {

    public static final ZoneId BOGOTA = ZoneId.of("America/Bogota");
    private static final DateTimeFormatter FORMATO = DateTimeFormatter
            .ofPattern("dd/MM/yyyy HH:mm")
            .withZone(BOGOTA);

    private ZonaHoraria() {
    }

    public static String formatear(Instant fecha) {
        return fecha == null ? "" : FORMATO.format(fecha);
    }

    public static LocalDate hoy() {
        return LocalDate.now(BOGOTA);
    }

    public static Instant inicioDelDia(LocalDate fecha) {
        return fecha.atStartOfDay(BOGOTA).toInstant();
    }

    /** Límite exclusivo para consultar un día completo sin depender del servidor. */
    public static Instant finExclusivoDelDia(LocalDate fecha) {
        return fecha.plusDays(1).atStartOfDay(BOGOTA).toInstant();
    }
}
