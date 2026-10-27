package pe.utp.rinconcito.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Renglón recibido; un producto puede tener varios lotes con distinto vencimiento. */
public record ReceiptLine(long productId, int quantity, BigDecimal cost, LocalDate expiration) {}
