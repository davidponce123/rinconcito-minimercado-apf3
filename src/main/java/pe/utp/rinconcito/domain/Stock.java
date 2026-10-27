package pe.utp.rinconcito.domain;

/** Existencia derivada de los movimientos y umbral de reposición. */
public record Stock(long productId, String name, long quantity, long minimum) {
    public boolean needsReorder() { return quantity <= minimum; }
}
