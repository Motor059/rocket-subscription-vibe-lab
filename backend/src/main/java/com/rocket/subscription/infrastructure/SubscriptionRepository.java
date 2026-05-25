package com.rocket.subscription.infrastructure;

import com.rocket.subscription.domain.Subscription;
import com.rocket.subscription.domain.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByStatus(SubscriptionStatus status);

    List<Subscription> findByUserId(Long userId);
}