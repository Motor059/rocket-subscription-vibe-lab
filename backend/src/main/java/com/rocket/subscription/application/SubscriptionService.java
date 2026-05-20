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
        
        // FIXME: [OOM 경고] 최근 90일 조건 누락. 전체 데이터 조회를 당장 멈추고 쿼리 최적화 필요.
        List<Transaction> transactions = transactionRepository.findByUserId(userId);

        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantName));

        for (String merchant : groupedByMerchant.keySet()) {
            List<Transaction> txList = groupedByMerchant.get(merchant);

                // FIXME: [환각 로직] 단순히 사이즈가 2 이상이라고 구독으로 판정하면 안 됨. 결제 주기(30일) 검증 로직 필요.            if (txList.size() >= 2) {
                Subscription sub = new Subscription();
                sub.setUser(txList.get(0).getUser());
                sub.setMerchantName(merchant);
                sub.setAmount(txList.get(0).getAmount());
                sub.setUnused(false);
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

            // 문제 3: FSM(상태 기계) 없이 단순히 플래그만 true로 바꾸어 나중에 기능 확장이 불가능함
            if (lastTx != null && lastTx.getTransactionDate().isBefore(thirtyDaysAgo)) {
                sub.setUnused(true);
            }
        }
    }
}