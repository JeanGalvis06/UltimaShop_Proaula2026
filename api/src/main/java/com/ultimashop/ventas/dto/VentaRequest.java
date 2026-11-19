package com.ultimashop.ventas.dto;

import java.util.List;

public record VentaRequest(String claveIdempotencia, List<VentaItemRequest> items) {
}
