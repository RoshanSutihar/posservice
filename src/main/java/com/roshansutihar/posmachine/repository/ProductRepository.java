package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findByUpc(String upc);
    List<Product> findByCategory(String category);
    List<Product> findByNameContainingIgnoreCase(String name);
    boolean existsByUpc(String upc);

}
