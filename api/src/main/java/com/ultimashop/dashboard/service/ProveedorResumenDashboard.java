package com.ultimashop.dashboard.service;

import com.ultimashop.dashboard.domain.ResumenDashboard;

/**
 * Punto de extensión del dashboard. La implementación provisional consulta las tablas
 * comerciales; cuando Lewis entregue el resumen asíncrono podrá sustituirse cambiando
 * la propiedad ultimashop.dashboard.fuente.
 */
public interface ProveedorResumenDashboard {

    ResumenDashboard obtener();
}
