package com.rocket.subscription.domain;

public enum SubscriptionStatus {
    DETECTED,  // 탐지됨 (사용자 확인 대기)
    CONFIRMED, // 확정됨 (정상 구독 중)
    WARNING,   // 경고 (결제 실패 등)
    CANCELED,  // 해지됨 (종료 상태)
    IGNORED    // 무시됨 (종료 상태)
}