package com.rocket.subscription.domain;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime; // 새로 추가된 임포트
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class SubscriptionDetector {

    // 한 달 정기결제로 인정할 최소/최대 일수
    private static final int MIN_MONTHLY_GAP_DAYS = 25;
    private static final int MAX_MONTHLY_GAP_DAYS = 40;
    
    // 유효 결제 내역 조회 기간 (90일) - 새로운 비즈니스 규칙 추가!
    private static final int VALID_PERIOD_DAYS = 90;

    public List<Subscription> analyze(User user, List<Transaction> transactions) {
        // [Scenario 7 방어] 빈 리스트가 들어오면 즉시 빈 결과 반환
        if (transactions == null || transactions.isEmpty()) {
            return Collections.emptyList();
        }

        // [NEW: Scenario 1 방어] 90일 경계값 필터링 (90일이 지난 과거 결제는 가차없이 버림)
        LocalDateTime boundaryDate = LocalDateTime.now().minusDays(VALID_PERIOD_DAYS);
        List<Transaction> recentTransactions = transactions.stream()
                .filter(tx -> !tx.getTransactionDate().isBefore(boundaryDate)) // 90일 경계 포함 (이전이 아닌 것만 통과)
                .collect(Collectors.toList());

        // 필터링 후 남은 유효 데이터가 2건 미만이면 정기구독 요건을 못 채우므로 조기 종료 (성능 최적화)
        if (recentTransactions.size() < 2) {
            return Collections.emptyList();
        }

        List<Subscription> detectedSubscriptions = new ArrayList<>();

        // 1. 가맹점명 기준 1차 그룹화 (기존 transactions 대신 필터링된 recentTransactions 사용!)
        Map<String, List<Transaction>> groupedByMerchant = recentTransactions.stream()
                .collect(Collectors.groupingBy(tx -> tx.getMerchantName().trim().toUpperCase()));

        for (Map.Entry<String, List<Transaction>> entry : groupedByMerchant.entrySet()) {
            String merchantName = entry.getKey();
            List<Transaction> txList = entry.getValue();
            
            if (txList.size() < 2) continue;

            // 결제일 순으로 오름차순 정렬
            txList.sort(Comparator.comparing(Transaction::getTransactionDate));

            // 2. 금액(Amount) 기준 2차 그룹화: ±10% 오차 허용 클러스터링
            List<List<Transaction>> amountClusters = new ArrayList<>();
            
            for (Transaction tx : txList) {
                boolean addedToCluster = false;
                
                for (List<Transaction> cluster : amountClusters) {
                    Transaction baseTx = cluster.get(0); 
                    double diffRatio = Math.abs((double) (tx.getAmount() - baseTx.getAmount()) / baseTx.getAmount());
                    
                    if (diffRatio <= 0.10) { 
                        cluster.add(tx);
                        addedToCluster = true;
                        break;
                    }
                }
                
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
                        detectedAmount = cluster.get(cluster.size() - 1).getAmount();
                        break; 
                    }
                }
                
                if (isPeriodic) break; 
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