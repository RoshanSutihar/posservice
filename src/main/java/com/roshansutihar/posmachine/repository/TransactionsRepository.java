package com.roshansutihar.posmachine.repository;

import com.roshansutihar.posmachine.entity.Transactions;
import com.roshansutihar.posmachine.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface TransactionsRepository extends JpaRepository<Transactions, Long> {

    // Updated this method to fetch all related data at once
    @Query("SELECT DISTINCT t FROM Transactions t " +
            "LEFT JOIN FETCH t.payments p " +
            "LEFT JOIN FETCH p.qrPayment q " +
            "LEFT JOIN FETCH t.transactionItems i " +
            "WHERE t.transactionDate BETWEEN :start AND :end " +
            "ORDER BY t.transactionDate DESC")
    List<Transactions> findByTransactionDateBetween(
            @Param("start") OffsetDateTime start,
            @Param("end") OffsetDateTime end);

}
