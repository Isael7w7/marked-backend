package com.merida.tecnm.mx.market.backend.domain.reposiroty;

import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface ProductRepository extends CrudRepository<Producto,Integer> {
    List<Producto> findbyIdCategoriaOrderByNombreAsc(int idCategoria)
        //
      Optional<List<Producto>  findByCantidadStockLessThanAndEstado(int cantidadStock , boolean estado),l
}
