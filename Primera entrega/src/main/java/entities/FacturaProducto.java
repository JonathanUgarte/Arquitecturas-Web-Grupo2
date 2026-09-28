package entities;

public class FacturaProducto {

    private int idFactura;
    private int idProducto;
    private int cantidad;

    public FacturaProducto(int idFactura, int idProducto, int cantidad){
        this.idFactura = idFactura;
        this.idProducto = idProducto;
        this.cantidad = cantidad;
    }

    public int getIdFactura() {return idFactura;}
    public void setIdFactura(int idFactura) {this.idFactura = idFactura;}

    public int getIdProducto() {return idProducto;}
    public void setIdProducto(int idProducto) {this.idProducto = idProducto;}

    public int getCantidad() {return cantidad;}
    public void setCantidad(int cantidad) {this.cantidad = cantidad;}
}
