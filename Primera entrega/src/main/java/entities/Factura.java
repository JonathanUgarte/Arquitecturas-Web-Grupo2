package entities;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@ToString
public class Factura {
    private int idFactura;
    private int idCliente;
}
