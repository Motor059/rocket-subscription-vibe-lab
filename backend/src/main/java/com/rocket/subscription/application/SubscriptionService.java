package com.rocket.subscription.application;

import com.rocket.subscription.domain.*;
import com.rocket.subscription.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    @Transactional
    public void detectSubscriptions(Long userId) {
        // 수정됨: 최근 90일 데이터만 가져옵니다.
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateAfter(userId, ninetyDaysAgo);

        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantName));

        for (String merchant : groupedByMerchant.keySet()) {
            List<Transaction> txList = groupedByMerchant.get(merchant);

            // 동일 가맹점에서 2회 이상 결제 시 구독으로 등록
            if (txList.size() >= 2) {
                Subscription sub = new Subscription();
                sub.setUser(txList.get(0).getUser());
                sub.setMerchantName(merchant);
                sub.setAmount(txList.get(0).getAmount());
                sub.setStatus(SubscriptionStatus.DETECTED); // 탐지됨 상태로 초기화
                subscriptionRepository.save(sub);
            }
        }
    }

    @Transactional
    public void checkUnusedSubscriptions() {
        List<Subscription> subscriptions = subscriptionRepository.findAll();
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        for (Subscription sub : subscriptions) {
            Transaction lastTx = transactionRepository.findTopByUserIdAndMerchantNameOrderByTransactionDateDesc(
                    sub.getUser().getId(), sub.getMerchantName());

            // 30일 이전 결제라면 WARNING 상태로 업데이트
            if (lastTx != null && lastTx.getTransactionDate().isBefore(thirtyDaysAgo)) {
                sub.setStatus(SubscriptionStatus.WARNING);
            }
        }
    }
}