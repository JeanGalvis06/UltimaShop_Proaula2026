package com.ultimashop.comun;

/**
 * Usuario autenticado que origina una venta o un movimiento.
 * Los registros anteriores al login conservan ambos datos en null.
 */
public record Auditoria(Long usuarioId, String usuarioNombre) {

    public static final Auditoria ANONIMA = new Auditoria(null, null);

    public String nombreParaAuditoria() {
        if (usuarioNombre == null) {
            return null;
        }
        String texto = usuarioNombre.trim();
        if (texto.isEmpty()) {
            return null;
        }
        return texto.length() > 100 ? texto.substring(0, 100) : texto;
    }
}
