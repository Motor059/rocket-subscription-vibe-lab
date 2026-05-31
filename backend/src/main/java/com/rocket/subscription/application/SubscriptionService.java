package com.rocket.subscription.application;

import com.rocket.subscription.application.dto.SubscriptionDetectedEvent;
import com.rocket.subscription.domain.*;
import com.rocket.subscription.infrastructure.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j // 관제탑 역할을 위한 로거 어노테이션 추가
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    // 리팩토링 1: 매직 넘버를 비즈니스 의미를 담은 상수로 분리
    private static final int DETECTION_PERIOD_DAYS = 90;
    private static final int UNUSED_WARNING_DAYS = 40;

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final SubscriptionDetector subscriptionDetector;
    
    // 리팩토링 2: 알림 서비스(NotificationService) 직접 의존성을 끊고, 이벤트 발행기 주입
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public void detectSubscriptions(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 유저입니다."));

        LocalDateTime boundaryDate = LocalDateTime.now().minusDays(DETECTION_PERIOD_DAYS);
        List<Transaction> transactions = transactionRepository
                .findByUserIdAndTransactionDateAfterOrderByTransactionDateAsc(userId, boundaryDate);

        List<Subscription> detectedList = subscriptionDetector.analyze(user, transactions);

        if (!detectedList.isEmpty()) {
            List<String> existingMerchants = subscriptionRepository.findByUserId(userId)
                    .stream()
                    .map(Subscription::getMerchantName)
                    .toList();

            List<Subscription> newSubscriptions = detectedList.stream()
                    .filter(sub -> !existingMerchants.contains(sub.getMerchantName()))
                    .toList();
                    
            if (!newSubscriptions.isEmpty()) {
                subscriptionRepository.saveAll(newSubscriptions);
                log.info("✅ [탐지 완료] 유저 ID: {} -> 새로운 정기구독 {}건 영속화 완료", userId, newSubscriptions.size());
                
                // 알림 서비스를 직접 호출하지 않고 이벤트만 발행 (결합도 완벽 분리!)
                eventPublisher.publishEvent(new SubscriptionDetectedEvent(userId, newSubscriptions));
            }
        }
    }

    @Transactional
    public void checkUnusedSubscriptions() {
        List<Subscription> detectedSubscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.DETECTED);
        LocalDateTime thresholdDate = LocalDateTime.now().minusDays(UNUSED_WARNING_DAYS);
        int warningCount = 0;

        for (Subscription sub : detectedSubscriptions) {
            Transaction lastTx = transactionRepository.findTopByUserIdAndMerchantNameIgnoreCaseOrderByTransactionDateDesc(
                    sub.getUser().getId(), sub.getMerchantName());

            if (lastTx != null && lastTx.getTransactionDate().isBefore(thresholdDate)) {
                sub.updateToWarning();
                warningCount++;
                log.warn("🚨 [FSM 상태 전이] 구독 ID: {} ({}) -> WARNING (미사용 의심)", sub.getId(), sub.getMerchantName());
            }
        }
        
        log.info("⏰ [스케줄러 동작 완료] 총 {}건의 미사용 구독을 체크 및 상태 변경했습니다.", warningCount);
    }

    @Transactional
    public void handleUserAction(Long subscriptionId, boolean isCancelAction) {
        Subscription subscription = subscriptionRepository.findById(subscriptionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 구독 내역입니다."));

        SubscriptionStatus previousStatus = subscription.getStatus();

        if (isCancelAction) {
            subscription.cancel(); 
        } else {
            subscription.ignore(); 
        }
        
        // 리팩토링 3: 유저의 액션에 의한 상태 전이를 추적하는 FSM 로깅 추가
        log.info("🕹️ [FSM 상태 전이] 사용자 액션 - 구독 ID: {} ({}) | {} ➔ {}", 
                subscription.getId(), subscription.getMerchantName(), previousStatus, subscription.getStatus());
    }
}