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

    // Case A의 치명적 결함: FSM 없이 단순히 boolean으로만 상태를 관리함
    private boolean isUnused;
}