package entities.dto;

import entities.Producto;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString(callSuper = true) // Incluye los campos del padre en el toString si lo necesitas
public class ProductoDTO extends Producto {
    private double recaudacion;

    // Como Lombok no hereda automáticamente los constructores,
    // creamos uno explícito que reciba los datos del padre y la recaudación
    public ProductoDTO(int idProducto, String nombre, float valor, double recaudacion) {
        super(idProducto, nombre, valor);
        this.recaudacion = recaudacion;
    }


}