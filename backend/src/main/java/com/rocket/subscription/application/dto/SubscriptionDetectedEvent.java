package com.rocket.subscription.application.dto;

import com.rocket.subscription.domain.Subscription;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import java.util.List;

@Getter
@RequiredArgsConstructor
public class SubscriptionDetectedEvent {
    private final Long userId;
    private final List<Subscription> detectedSubscriptions;
}