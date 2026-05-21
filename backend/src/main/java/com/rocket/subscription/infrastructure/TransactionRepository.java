package com.rocket.subscription.infrastructure;

import com.rocket.subscription.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // OOM 방지: 특정 날짜(90일 전) 이후의 데이터만 조회하는 메서드로 변경
    List<Transaction> findByUserIdAndTransactionDateAfter(Long userId, LocalDateTime startDate);

    Transaction findTopByUserIdAndMerchantNameOrderByTransactionDateDesc(Long userId, String merchantName);
}