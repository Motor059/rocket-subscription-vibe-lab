package com.rocket.subscription.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 무분별한 기본 생성자 사용 차단
public class Subscription {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String merchantName;

    @Column(nullable = false)
    private int amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubscriptionStatus status;

    // 1. 생성 로직 (초기 상태는 무조건 DETECTED)
    public static Subscription createDetected(User user, String merchantName, int amount) {
        Subscription subscription = new Subscription();
        subscription.user = user;
        subscription.merchantName = merchantName;
        subscription.amount = amount;
        subscription.status = SubscriptionStatus.DETECTED;
        return subscription;
    }

    // 2. FSM 상태 전이 메서드 (캡슐화 및 방어 로직 적용)

    public void updateToWarning() {
        if (this.status != SubscriptionStatus.DETECTED) {
            throw new IllegalStateException("오직 DETECTED 상태에서만 WARNING으로 전이할 수 있습니다.");
        }
        this.status = SubscriptionStatus.WARNING;
    }

    public void cancel() {
        if (this.status != SubscriptionStatus.WARNING) {
            throw new IllegalStateException("오직 WARNING 상태에서만 CANCELED(해지)로 전이할 수 있습니다.");
        }
        this.status = SubscriptionStatus.CANCELED;
    }

    public void ignore() {
        if (this.status == SubscriptionStatus.CANCELED || this.status == SubscriptionStatus.IGNORED) {
            throw new IllegalStateException("이미 종료된 상태(CANCELED/IGNORED)에서는 상태를 변경할 수 없습니다.");
        }
        this.status = SubscriptionStatus.IGNORED;
    }
}