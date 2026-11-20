package com.merida.tecnm.mx.market.backend.persistence.crud;

import com.merida.tecnm.mx.market.backend.persistence.entity.Categoria;
import org.springframework.data.repository.CrudRepository;

public interface CategoriaCrudRepository extends CrudRepository<Categoria, Integer> {
}