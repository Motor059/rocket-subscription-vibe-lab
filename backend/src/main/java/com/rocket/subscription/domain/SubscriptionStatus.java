package com.rocket.subscription.domain;

public enum SubscriptionStatus {
    DETECTED,  // 탐지됨
    WARNING,   // 경고 (미사용)
    CANCELED,  // 해지됨 (종료 상태)
    IGNORED    // 무시됨 (종료 상태)
}

// Case A와 동일