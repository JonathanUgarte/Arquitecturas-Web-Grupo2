package entities;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class Cliente {
    private int idCliente;
    private String nombre;
    private String email;


    @Override
    public String toString() {
        return "\n|ID= " + idCliente +
                "\n|Nombre= " + nombre +
                "\n|Email= " + email;
    }
}
