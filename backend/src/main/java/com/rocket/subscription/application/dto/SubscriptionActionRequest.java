package com.rocket.subscription.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class SubscriptionActionRequest {

    @JsonProperty("isCancelAction")
    private boolean isCancelAction; // 좌/우 스와이프 분기 플래그
}