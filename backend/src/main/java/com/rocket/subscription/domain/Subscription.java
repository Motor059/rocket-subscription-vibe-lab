package com.rocket.subscription.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter @Setter
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    private String merchantName;
    private int amount;

    // 기존 boolean 플래그 대신 Enum 상태값 적용
    @Enumerated(EnumType.STRING)
    private SubscriptionStatus status;
}