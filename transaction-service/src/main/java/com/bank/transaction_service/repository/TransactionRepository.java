package com.bank.transaction_service.repository;

import com.bank.transaction_service.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {
    List<Transaction> findBySenderAccountNumberOrReceiverAccountNumber(String sender, String receiver);
}
