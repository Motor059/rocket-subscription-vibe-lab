package com.rocket.subscription.domain;

import org.springframework.stereotype.Component;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
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
        // [Scenario 7 방어] 빈 리스트가 들어오면 즉시 빈 결과 반환 (NPE 방지)
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        List<Subscription> detectedSubscriptions = new ArrayList<>();

        // 1. 가맹점명 기준 1차 그룹화 (대소문자 통일 및 양옆 공백 제거 정규화 적용)
        Map<String, List<Transaction>> groupedByMerchant = transactions.stream()
                .collect(Collectors.groupingBy(tx -> tx.getMerchantName().trim().toUpperCase()));

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            String merchantName = entry.getKey();
            List<Transaction> txList = entry.getValue();
            
            if (txList.size() < 2) continue;

            // 결제일 순으로 오름차순 정렬 (미리 정렬해야 주기 계산 및 최신 금액 추출이 정확해짐)
            txList.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 2. 금액(Amount) 기준 2차 그룹화: ±10% 오차 허용 클러스터링 적용
            List<List<Transaction>> amountClusters = new ArrayList<>();
            
            for (Transaction tx : txList) {
                boolean addedToCluster = false;
                
                for (List<Transaction> cluster : amountClusters) {
                    Transaction baseTx = cluster.get(0); // 그룹의 첫 결제 금액이 기준점
                    // 오차율 = |현재금액 - 기준금액| / 기준금액
                    double diffRatio = Math.abs((double) (tx.getAmount() - baseTx.getAmount()) / baseTx.getAmount());
                    
                    if (diffRatio <= 0.10) { // 10% 이내 오차라면 같은 정기결제 그룹으로 인정
                        cluster.add(tx);
                        addedToCluster = true;
                        break;
                    }
                }
                
                // 어떤 그룹에도 속하지 못했다면(오차가 10% 이상이라면) 새로운 결제 그룹 생성
                if (!addedToCluster) {
                    List<Transaction> newCluster = new ArrayList<>();
                    newCluster.add(tx);
                    amountClusters.add(newCluster);
                }
            }

            boolean isPeriodic = false;
            int detectedAmount = 0;

            // 3. 유동적인 주기 검증 (25~40일)
            for (List<Transaction> cluster : amountClusters) {
                if (cluster.size() < 2) continue;

                for (int i = 0; i < cluster.size() - 1; i++) {
                    Transaction current = cluster.get(i);
                    Transaction next = cluster.get(i + 1);

                    long daysBetween = ChronoUnit.DAYS.between(current.getTransactionDate(), next.getTransactionDate());

                    if (daysBetween >= MIN_MONTHLY_GAP_DAYS && daysBetween <= MAX_MONTHLY_GAP_DAYS) {
                        isPeriodic = true;
                        // 주기성을 찾으면, 해당 그룹의 "가장 최신 결제 금액"으로 구독 금액을 갱신 (인상분 반영)
                        detectedAmount = cluster.get(cluster.size() - 1).getAmount();
                        break; 
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