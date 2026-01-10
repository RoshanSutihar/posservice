package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {
    List<TransactionItem> findByTransactionId(Long transactionId);
    List<TransactionItem> findByProductId(Long productId);

    @Query("SELECT ti FROM TransactionItem ti WHERE ti.transaction.id = :transactionId")
    List<TransactionItem> findItemsByTransaction(@Param("transactionId") Long transactionId);

    @Query("SELECT SUM(ti.quantity) FROM TransactionItem ti WHERE ti.product.id = :productId")
    Long getTotalQuantitySoldByProduct(@Param("productId") Long productId);
}
