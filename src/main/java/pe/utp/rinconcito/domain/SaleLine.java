package pe.utp.rinconcito.domain;

/** Ítem solicitado para una venta; las cantidades son enteras y positivas. */
public record SaleLine(long productId, int quantity) {}
