package com.rocket.subscription.application;

import com.rocket.subscription.domain.*;
import com.rocket.subscription.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
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
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Transaction> transactions = transactionRepository.findByUserIdAndTransactionDateAfter(userId, ninetyDaysAgo);

        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantName));

        for (String merchant : groupedByMerchant.keySet()) {
            List<Transaction> txList = groupedByMerchant.get(merchant);

            // 데이터가 2개 미만이면 검사 안함
            if (txList.size() < 2) continue;

            // 1. 결제일 기준으로 오름차순 정렬
            txList.sort(Comparator.comparing(Transaction::getTransactionDate));

            boolean isPeriodic = false;
            Transaction targetTx = null;

            // 2. 금액 동일 여부 및 결제 간격(28~31일) 수학적 검증
            for (int i = 0; i < txList.size() - 1; i++) {
                Transaction current = txList.get(i);
                Transaction next = txList.get(i + 1);

                long daysBetween = ChronoUnit.DAYS.between(current.getTransactionDate(), next.getTransactionDate());

                if (current.getAmount() == next.getAmount() && daysBetween >= 28 && daysBetween <= 31) {
                    isPeriodic = true;
                    targetTx = current; // 기준 결제 내역 저장
                    break;
                }
            }

            // 3. 주기적 결제 패턴이 확인된 경우에만 구독 객체 생성
            if (isPeriodic) {
                Subscription sub = new Subscription();
                sub.setUser(targetTx.getUser());
                sub.setMerchantName(merchant);
                sub.setAmount(targetTx.getAmount());
                sub.setStatus(SubscriptionStatus.DETECTED);
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

            // 여전히 남아있는 구조적 부채: 취소(CANCELED) 상태인 것도 무조건 WARNING으로 덮어씌움
            if (lastTx != null && lastTx.getTransactionDate().isBefore(thirtyDaysAgo)) {
                sub.setStatus(SubscriptionStatus.WARNING);
            }
        }
    }
}