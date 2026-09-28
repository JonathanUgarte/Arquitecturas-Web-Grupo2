package entities.dto;

import entities.Cliente;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClienteDTO extends Cliente {

    private double  total_facturado;

    public ClienteDTO(int idCliente, String nombre, String email, double total_facturado){
        super(idCliente,nombre,email);
        this.total_facturado = total_facturado;
    }

    @Override
    public String toString() {
        return super.toString() +
                "\n|Total facturado= " + total_facturado;
    }
}
