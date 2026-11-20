package com.merida.tecnm.mx.market.backend.domain.repository;

import com.merida.tecnm.mx.market.backend.domain.Purchase;

import java.util.List;
import java.util.Optional;

public interface PurchaseRepository {

    List<Purchase> getAll();
    Optional<List<Purchase>> getByClient(String clienteId);
    Purchase save(Purchase purchase);
}
