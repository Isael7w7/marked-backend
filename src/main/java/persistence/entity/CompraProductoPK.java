package persistence.entity;

import jakarta.persistence.Column;

import java.io.Serializable;
import jakarta.persistence.Embeddable;
import java.time.LocalDateTime;
@Embeddable
public class CompraProductoPK implements Serializable {
   private Integer idCompra;

   @Column(name = "id_compra")
    private Integer idCompra;

    @Column(name = "id_producto")
    private Integer idproducto;


}
