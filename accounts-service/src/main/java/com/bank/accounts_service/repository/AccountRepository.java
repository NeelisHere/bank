package com.bank.accounts_service.repository;

import com.bank.accounts_service.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {
    Optional<Account> findByAccountNumber(String accountNumber);
    boolean existsByAccountNumber(String accountNumber);
    Optional<Account> findByEmail(String email);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance + :amount WHERE a.accountNumber = :accountNumber")
    void creditBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);

    @Modifying
    @Query("UPDATE Account a SET a.balance = a.balance - :amount WHERE a.accountNumber = :accountNumber")
    void deductBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
