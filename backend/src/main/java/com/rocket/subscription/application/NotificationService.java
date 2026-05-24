package com.rocket.subscription.application;

import com.rocket.subscription.domain.Subscription;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.util.List;

@Slf4j
@Service
public class NotificationService {

    public void sendAlert(Long userId, List<Subscription> detectedList) {
        for (Subscription sub : detectedList) {
            log.info("🔔 [알림 발송] 사용자 ID: {} -> 미사용 의심 구독 탐지: {} (금액: {}원)",
                    userId, sub.getMerchantName(), sub.getAmount());
        }
    }
}