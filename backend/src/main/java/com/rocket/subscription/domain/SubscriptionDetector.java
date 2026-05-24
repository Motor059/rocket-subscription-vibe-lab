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

    // 한 달 정기결제로 인정할 최소/최대 일수 (영업일 이월 및 2월 일수 고려)
    private static final int MIN_MONTHLY_GAP_DAYS = 25;
    private static final int MAX_MONTHLY_GAP_DAYS = 40;

    public List<Subscription> analyze(User user, List<Transaction> transactions) {
        List<Subscription> detectedSubscriptions = new ArrayList<>();

        // 1. 가맹점명 기준 1차 그룹화
        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(Transaction::getMerchantName));

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            String merchantName = entry.getKey();
            List<Transaction> txList = entry.getValue();
            
            if (txList.size() < 2) continue;

            // 2. 금액(Amount) 기준 2차 그룹화
            // 정기 구독은 "동일한 금액"이 반복된다는 강력한 특징이 있음
            // 일반 단건 결제(예: 쿠팡 쇼핑)가 섞여 있어도 금액으로 분리해내기 위함
            Map<Integer, List<Transaction>> groupedByAmount = txList.stream()
                    .collect(Collectors.groupingBy(Transaction::getAmount));

            boolean isPeriodic = false;
            int detectedAmount = 0;

            for (Map.Entry<Integer, List<Transaction>> amountEntry : groupedByAmount.entrySet()) {
                List<Transaction> sameAmountTxs = amountEntry.getValue();
                if (sameAmountTxs.size() < 2) continue;

                // 결제일 기준 오름차순 정렬
                sameAmountTxs.sort(Comparator.comparing(Transaction::getTransactionDate));

                // 3. 유동적인 주기 검증 (25~40일)
                for (int i = 0; i < sameAmountTxs.size() - 1; i++) {
                    Transaction current = sameAmountTxs.get(i);
                    Transaction next = sameAmountTxs.get(i + 1);

                    long daysBetween = ChronoUnit.DAYS.between(current.getTransactionDate(), next.getTransactionDate());

                    if (daysBetween >= MIN_MONTHLY_GAP_DAYS && daysBetween <= MAX_MONTHLY_GAP_DAYS) {
                        isPeriodic = true;
                        detectedAmount = current.getAmount();
                        break; // 주기성을 하나라도 찾으면 해당 금액으로 구독 확정
                    }
                }
                
                if (isPeriodic) break; // 이미 구독으로 판별되었으면 다른 금액 그룹은 검사 불필요
            }

            // 4. 조건 충족 시 DETECTED 상태의 구독 객체 생성
            if (isPeriodic) {
                detectedSubscriptions.add(
                        Subscription.createDetected(user, merchantName, detectedAmount)
                );
            }
        }

        return detectedSubscriptions;
    }
}