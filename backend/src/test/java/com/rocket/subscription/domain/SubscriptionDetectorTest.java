package com.rocket.subscription.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriptionDetectorTest {

    private final SubscriptionDetector detector = new SubscriptionDetector();

    @Test
    @DisplayName("Scenario 7: 결제 내역이 전혀 없는 유저일 경우 예외(NPE) 없이 빈 리스트를 반환한다.")
    void test_empty_transactions() {
        // given
        User user = User.create("테스트유저");
        List<Transaction> emptyList = Collections.emptyList();

        // when
        List<Subscription> result = detector.analyze(user, emptyList);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Scenario 4: 가맹점명에 대소문자/공백 오타가 있어도 하나의 구독으로 통합 탐지해야 한다.")
    void test_merchant_name_normalization() {
        // given
        User user = User.create("테스트유저");
        LocalDateTime now = LocalDateTime.now();

        // Netflix, netflix, ' NETFLIX ' 로 지저분하게 데이터가 들어온 상황 가정
        List<Transaction> transactions = List.of(
                Transaction.create(user, "Netflix", 13500, now.minusDays(60)),
                Transaction.create(user, "netflix", 13500, now.minusDays(30)),
                Transaction.create(user, " NETFLIX ", 13500, now)
        );

        // when
        List<Subscription> result = detector.analyze(user, transactions);

        // then
        // 3건이 1개의 정기 구독(NETFLIX)으로 묶여야 하므로 size는 1이어야 함
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getMerchantName()).isEqualTo("NETFLIX");
    }

    @Test
    @DisplayName("Scenario 2: 결제 금액이 ±10% 이내로 변경되어도 하나의 정기 구독으로 묶여야 한다.")
    void test_amount_tolerance() {
        // given
        User user = User.create("테스트유저");
        LocalDateTime now = LocalDateTime.now();

        // 딱 2건만 넣습니다. 요금이 변경된 경우! (10,000원 -> 10,500원 5% 인상)
        List<Transaction> transactions = List.of(
                Transaction.create(user, "NETFLIX", 10000, now.minusDays(30)),
                Transaction.create(user, "NETFLIX", 10500, now)
        );

        // when
        List<Subscription> result = detector.analyze(user, transactions);

        // then
        // 금액이 다르더라도 오차(10%) 이내이므로 1개의 구독으로 묶여야 합니다.
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAmount()).isEqualTo(10500);
    }
}