package com.rocket.subscription.application.dto;

import com.rocket.subscription.domain.Subscription;
import com.rocket.subscription.domain.SubscriptionStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class SubscriptionResponse {
    private Long id;
    private String merchantName;
    private int amount;
    private SubscriptionStatus status;
    private LocalDateTime lastTransactionDate;

    public static SubscriptionResponse of(Subscription subscription, LocalDateTime lastTransactionDate) {
        return new SubscriptionResponse(
                subscription.getId(),
                subscription.getMerchantName(),
                subscription.getAmount(),
                subscription.getStatus(),
                lastTransactionDate
        );
    }
}