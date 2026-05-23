package com.rocket.subscription.infrastructure;

import com.rocket.subscription.domain.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // [OOM 원천 차단] 특정 사용자의 기준일(90일 전) 이후 결제 내역만 시간순으로 조회
    List<Transaction> findByUserIdAndTransactionDateAfterOrderByTransactionDateAsc(Long userId, LocalDateTime startDate);

    // 특정 가맹점의 가장 최근 결제 내역 조회 (미사용 구독 검증용)
    Transaction findTopByUserIdAndMerchantNameOrderByTransactionDateDesc(Long userId, String merchantName);
}