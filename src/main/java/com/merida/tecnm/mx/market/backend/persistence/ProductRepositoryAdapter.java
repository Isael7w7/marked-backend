package com.merida.tecnm.mx.market.backend.persistence;

import com.merida.tecnm.mx.market.backend.domain.Product;
import com.merida.tecnm.mx.market.backend.domain.repository.ProductRepository;
import com.merida.tecnm.mx.market.backend.persistence.crud.ProductoCrudRepository;
import com.merida.tecnm.mx.market.backend.persistence.entity.Producto;
import com.merida.tecnm.mx.market.backend.persistence.mapper.ProductMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@Repository
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductoCrudRepository productoCrudRepository;
    private final ProductMapper productMapper;

    public ProductRepositoryAdapter(ProductoCrudRepository productoCrudRepository, ProductMapper productMapper) {
        this.productoCrudRepository = productoCrudRepository;
        this.productMapper = productMapper;
    }

    @Override
    public List<Product> getAll() {
        List<Producto> productos = StreamSupport.stream(productoCrudRepository.findAll().spliterator(), false).toList();
        return productMapper.toProducts(productos);
    }

    @Override
    public Optional<List<Product>> getByCategory(int categoryId) {
        List<Product> products = productMapper.toProducts(
                productoCrudRepository.findByIdCategoriaOrderByNombreAsc(categoryId));
        return products.isEmpty() ? Optional.empty() : Optional.of(products);
    }

    @Override
    public Optional<List<Product>> getScarceProducts(int quantity) {
        return productoCrudRepository.findByCantidadStockLessThanAndEstado(quantity, true)
                .map(productMapper::toProducts)
                .filter(products -> !products.isEmpty());
    }

    @Override
    public Optional<Product> getProduct(int productId) {
        return productoCrudRepository.findById(productId).map(productMapper::toProduct);
    }

    @Override
    public Product save(Product product) {
        return productMapper.toProduct(productoCrudRepository.save(productMapper.toProducto(product)));
    }

    @Override
    public void delete(int productId) {
        productoCrudRepository.deleteById(productId);
    }
}
