package entities;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Producto {
    private int idProducto;
    private String nombre;
    private float valor;

    @Override
    public String toString() {
        return
                "|ID= " + idProducto +
                "\n|Nombre= " + nombre +
                "\n|Valor= " + valor;
    }
}
