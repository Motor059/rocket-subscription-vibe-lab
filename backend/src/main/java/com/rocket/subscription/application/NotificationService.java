package com.rocket.subscription.application;

import com.rocket.subscription.application.dto.SubscriptionDetectedEvent;
import com.rocket.subscription.domain.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Service
public class NotificationService {

    // 핵심: 트랜잭션이 성공적으로 커밋된 직후에만 알림을 발송합니다!
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSubscriptionDetectedEvent(SubscriptionDetectedEvent event) {
        for (Subscription sub : event.getDetectedSubscriptions()) {
            log.info("🔔 [알림 발송] 사용자 ID: {} -> 새로운 구독 탐지 완료: {} (금액: {}원)",
                    event.getUserId(), sub.getMerchantName(), sub.getAmount());
        }
    }
}