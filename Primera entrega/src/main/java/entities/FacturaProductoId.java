package entities;

// Clave primaria compuesta de factura_producto
public record FacturaProductoId(int idFactura, int idProducto) {
}
