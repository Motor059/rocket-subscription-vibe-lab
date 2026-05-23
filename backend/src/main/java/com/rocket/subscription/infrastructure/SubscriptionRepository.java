package com.rocket.subscription.infrastructure;

import com.rocket.subscription.domain.Subscription;
import com.rocket.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    // 상태 변경 검사를 위해 DETECTED 상태인 구독만 조회
    List<Subscription> findAllByStatus(SubscriptionStatus status);
}