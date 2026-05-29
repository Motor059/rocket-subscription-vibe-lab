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
        // 1. 방어 로직: 이제 막 탐지된(DETECTED) 상태에서 강제 해지는 불가 (테스트 통과를 위한 핵심 방어선)
        if (this.status == SubscriptionStatus.DETECTED) {
            throw new IllegalStateException("아직 확정되지 않은 구독은 해지할 수 없습니다. 구독 무시(IGNORE)를 이용해주세요.");
        }
        
        // 2. 방어 로직: 이미 종료된 상태(CANCELED, IGNORED)에서 또 해지 시도 불가
        if (this.status == SubscriptionStatus.CANCELED || this.status == SubscriptionStatus.IGNORED) {
            throw new IllegalStateException("이미 종료된 구독입니다.");
        }

        // 3. 정상 처리: 활성화(CONFIRMED)되거나 경고(WARNING) 상태인 경우에만 해지 허용
        this.status = SubscriptionStatus.CANCELED;
    } 

    public void ignore() {
        if (this.status == SubscriptionStatus.CANCELED || this.status == SubscriptionStatus.IGNORED) {
            throw new IllegalStateException("이미 종료된 상태(CANCELED/IGNORED)에서는 상태를 변경할 수 없습니다.");
        }
        this.status = SubscriptionStatus.IGNORED;
    }
}