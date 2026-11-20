package com.merida.tecnm.mx.market.backend.persistence;

import com.merida.tecnm.mx.market.backend.domain.Purchase;
import com.merida.tecnm.mx.market.backend.domain.repository.PurchaseRepository;
import com.merida.tecnm.mx.market.backend.persistence.crud.CompraCrudRepository;
import com.merida.tecnm.mx.market.backend.persistence.crud.ProductoCrudRepository;
import com.merida.tecnm.mx.market.backend.persistence.entity.Compra;
import com.merida.tecnm.mx.market.backend.persistence.entity.CompraProducto;
import com.merida.tecnm.mx.market.backend.persistence.entity.Producto;
import com.merida.tecnm.mx.market.backend.persistence.mapper.PurchaseMapper;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public class PurchaseRepositoryAdapter implements PurchaseRepository {

    private final CompraCrudRepository compraCrudRepository;
    private final ProductoCrudRepository productoCrudRepository;
    private final PurchaseMapper purchaseMapper;

    public PurchaseRepositoryAdapter(
            CompraCrudRepository compraCrudRepository,
            ProductoCrudRepository productoCrudRepository,
            PurchaseMapper purchaseMapper) {
        this.compraCrudRepository = compraCrudRepository;
        this.productoCrudRepository = productoCrudRepository;
        this.purchaseMapper = purchaseMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Purchase> getAll() {
        List<Compra> compras = toList(compraCrudRepository.findAll());
        return purchaseMapper.toPurchases(compras);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<List<Purchase>> getByClient(String clienteId) {
        int idCliente = Integer.parseInt(clienteId);
        List<Purchase> purchases = purchaseMapper.toPurchases(
                compraCrudRepository.findByIdClienteOrderByFechaDesc(idCliente));
        return purchases.isEmpty() ? Optional.empty() : Optional.of(purchases);
    }

    @Override
    @Transactional
    public Purchase save(Purchase purchase) {
        Compra compra = purchaseMapper.toCompra(purchase);
        if (compra.getProductos() != null) {
            for (CompraProducto item : compra.getProductos()) {
                item.setCompra(compra);
                Integer productId = item.getId() == null ? null : item.getId().getIdProducto();
                if (productId == null) {
                    throw new IllegalArgumentException("Every purchase item must specify a product ID");
                }
                Producto producto = productoCrudRepository.findById(productId)
                        .orElseThrow(() -> new EntityNotFoundException("Product not found: " + productId));
                item.setProducto(producto);
            }
        }
        return purchaseMapper.toPurchase(compraCrudRepository.save(compra));
    }

    private List<Compra> toList(Iterable<Compra> compras) {
        java.util.ArrayList<Compra> result = new java.util.ArrayList<>();
        compras.forEach(result::add);
        return result;
    }
}
