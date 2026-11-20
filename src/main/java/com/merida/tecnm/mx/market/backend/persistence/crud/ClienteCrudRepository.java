package com.merida.tecnm.mx.market.backend.persistence.crud;

import com.merida.tecnm.mx.market.backend.persistence.entity.Cliente;
import org.springframework.data.repository.CrudRepository;

public interface ClienteCrudRepository extends CrudRepository<Cliente, Integer> {
}