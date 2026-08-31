package entities;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Producto {
    private int idProducto;
    private String nombre;
    private float valor;
}
