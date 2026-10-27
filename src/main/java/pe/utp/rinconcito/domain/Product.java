package pe.utp.rinconcito.domain;

import java.math.BigDecimal;

/** Producto registrado en el catálogo de Rinconcito. */
public record Product(long id, String name, BigDecimal price, String category, boolean active) {}
