package com.rocket.subscription.application;

import com.rocket.subscription.domain.*;
import com.rocket.subscription.infrastructure.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SubscriptionService {

    private final UserRepository userRepository; // User 조회를 위한 추가
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;

    // 협력 객체 주입 (SD1 스펙 반영)
    private final SubscriptionDetector subscriptionDetector;
    private final NotificationService notificationService;

    /**
     * SD1 시퀀스 완전히 일치: 구독 자동 탐지 및 알림 흐름
     */
    @Transactional
    public void detectSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        // 1. TransactionRepository -> Scheduler(Service) : 최근 90일 내역 리턴
        LocalDateTime ninetyDaysAgo = LocalDateTime.now().minusDays(90);
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateAfterOrderByTransactionDateAsc(userId, ninetyDaysAgo);

        // 2. Scheduler(Service) -> SubscriptionDetector : 분석 위임
        List<Subscription> detectedList = subscriptionDetector.analyze(user, transactions);

        // 3. 탐지된 내역이 있다면 영속화 및 알림 서비스 호출
        if (!detectedList.isEmpty()) {
            subscriptionRepository.saveAll(detectedList);

            // 4. Scheduler(Service) -> NotificationService : 알림 발송
            notificationService.sendAlert(userId, detectedList);
        }
    }

    @Transactional
    public void checkUnusedSubscriptions() {
        List<Subscription> detectedSubscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.DETECTED);
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        for (Subscription sub : detectedSubscriptions) {
            Transaction lastTx = transactionRepository.findTopByUserIdAndMerchantNameOrderByTransactionDateDesc(
                    sub.getUser().getId(), sub.getMerchantName());

            if (lastTx != null && lastTx.getTransactionDate().isBefore(thirtyDaysAgo)) {
                sub.updateToWarning(); // FSM 보호 조건 검증 후 상태 변경
            }
        }
    }

    @Transactional
    public void handleUserAction(Long subscriptionId, boolean isCancelAction) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 내역입니다."));

        // alt [direction == LEFT / RIGHT] 분기 처리
        if (isCancelAction) {
            subscription.cancel(); // CANCELED 전이
        } else {
            subscription.ignore(); // IGNORED 전이
        }
    }
}