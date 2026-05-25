package com.rocket.subscription.application.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionActionRequest {
    private boolean isCancelAction; // 좌/우 스와이프 분기 플래그
}