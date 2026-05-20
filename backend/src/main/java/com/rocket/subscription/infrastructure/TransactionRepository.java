package com.rocket.subscription.infrastructure;

import com.rocket.subscription.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Case A의 치명적 결함: 기간 제한 없이 모든 데이터를 가져와 OOM(메모리 초과) 유발 위험
    List<Transaction> findByUserId(Long userId);

    Transaction findTopByUserIdAndMerchantNameOrderByTransactionDateDesc(Long userId, String merchantName);
}