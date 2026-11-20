package com.merida.tecnm.mx.market.backend.persistence.crud;

import com.merida.tecnm.mx.market.backend.persistence.entity.Compra;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface CompraCrudRepository extends CrudRepository<Compra, Integer> {

    List<Compra> findByIdClienteOrderByFechaDesc(Integer idCliente);
}
