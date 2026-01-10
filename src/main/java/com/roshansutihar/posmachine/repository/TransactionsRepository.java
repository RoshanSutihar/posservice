package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.Transactions;
import com.roshansutihar.posmachine.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {
    List<Transactions> findByTransactionType(TransactionType transactionType);
    List<Transactions> findByTransactionDateBetween(OffsetDateTime start, OffsetDateTime end);
    List<Transactions> findByTotalAmountGreaterThan(BigDecimal amount);

    @Query("SELECT t FROM Transactions t ORDER BY t.transactionDate DESC")
    List<Transactions> findAllOrderByDateDesc();
}
