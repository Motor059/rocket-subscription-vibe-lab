package com.rocket.subscription.domain;

import org.springframework.stereotype.Component;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SubscriptionDetector {

    public List<Subscription> analyze(User user, List<Transaction> transactions) {
        List<Subscription> detectedSubscriptions = new ArrayList<>();

        // 가맹점명 기준 그룹화
        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantName));

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            List<Transaction> txList = entry.getValue();
            if (txList.size() < 2) continue;

            // 결제일 기준 오름차순 정렬
            txList.sort(Comparator.comparing(Transaction::getTransactionDate));

            boolean isPeriodic = false;
            Transaction targetTx = null;

            // 주기 및 금액 수학적 검증 (28~31일 주기, 금액 일치)
            for (int i = 0; i < txList.size() - 1; i++) {
                Transaction current = txList.get(i);
                Transaction next = txList.get(i + 1);

                long daysBetween = ChronoUnit.DAYS.between(current.getTransactionDate(), next.getTransactionDate());

                if (current.getAmount() == next.getAmount() && daysBetween >= 28 && daysBetween <= 31) {
                    isPeriodic = true;
                    targetTx = current;
                    break;
                }
            }

            // 조건 충족 시 DETECTED 상태의 구독 객체 생성
            if (isPeriodic) {
                detectedSubscriptions.add(
                        Subscription.createDetected(user, entry.getKey(), targetTx.getAmount())
                );
            }
        }

        return detectedSubscriptions;
    }
}