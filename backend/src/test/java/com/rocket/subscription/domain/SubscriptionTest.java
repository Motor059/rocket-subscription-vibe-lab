package com.rocket.subscription.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SubscriptionTest {

    @Test
    @DisplayName("Scenario 5: 도메인 규칙에 어긋나는 비정상적인 상태 전이 시 예외(IllegalStateException)로 방어해야 한다.")
    void test_fsm_state_transition_defense() {
        // given
        User user = User.create("테스트유저");
        
        // 탐지기(Detector)에 의해 갓 '탐지된(DETECTED)' 상태의 구독 객체 생성
        Subscription subscription = Subscription.createDetected(user, "NETFLIX", 10000);

        // when & then
        // 사용자가 확정(CONFIRMED)을 누르지도 않았는데, 외부에서 강제로 해지(CANCELED) 상태로 바꾸려 시도함
        // 이 경우 엔티티가 이를 거부하고 IllegalStateException을 던져야 정상(Green)임!
        assertThatThrownBy(() -> subscription.cancel())
                .isInstanceOf(IllegalStateException.class);
    }
}