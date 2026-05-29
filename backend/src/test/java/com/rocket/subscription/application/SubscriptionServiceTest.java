package com.rocket.subscription.application;

import com.rocket.subscription.domain.Subscription;
import com.rocket.subscription.domain.Transaction;
import com.rocket.subscription.domain.User;
import com.rocket.subscription.infrastructure.SubscriptionRepository;
import com.rocket.subscription.infrastructure.TransactionRepository;
import com.rocket.subscription.infrastructure.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest // 스프링 컨테이너와 가짜 DB(H2)를 통째로 띄우는 강력한 어노테이션
@Transactional  // 테스트가 끝나면 DB에 넣었던 데이터를 깔끔하게 롤백(삭제)해 줌
class SubscriptionServiceTest {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    @DisplayName("E2E Scenario: 사용자의 결제 내역을 분석하여 새로운 정기구독을 DB에 저장해야 한다.")
    void test_detect_and_save_subscriptions() {
        // given: 실제 DB(H2)에 유저와 결제 내역을 밀어넣음 (순수 자바 객체가 아님!)
        User user = User.create("통합테스트유저");
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();
        // 30일 간격 결제 2건 (정기구독 조건 충족)
        Transaction tx1 = Transaction.create(user, "NETFLIX", 10000, now.minusDays(30));
        Transaction tx2 = Transaction.create(user, "NETFLIX", 10000, now);
        transactionRepository.saveAll(List.of(tx1, tx2));

        // when: 서비스 레이어의 핵심 메서드 실행 (DB 조회 -> 탐지기 분석 -> DB 저장 -> 알림 발송)
        subscriptionService.detectSubscriptions(user.getId());

        // then: DB에 실제로 넷플릭스 구독이 1건 저장되었는지 Repository를 통해 검증
        List<Subscription> savedSubscriptions = subscriptionRepository.findByUserId(user.getId());
        
        assertThat(savedSubscriptions).hasSize(1);
        assertThat(savedSubscriptions.get(0).getMerchantName()).isEqualTo("NETFLIX");
        assertThat(savedSubscriptions.get(0).getAmount()).isEqualTo(10000);
        assertThat(savedSubscriptions.get(0).getStatus().name()).isEqualTo("DETECTED");
    }
    
    @Test
    @DisplayName("E2E Scenario: 미사용 구독 스케줄러 실행 시, 40일 이상 결제가 없는 구독은 WARNING 상태로 변경되어야 한다.")
    void test_check_unused_subscriptions() {
        // given: 45일 전에 마지막으로 결제된 유령 구독 내역 세팅
        User user = User.create("스케줄러테스트유저");
        userRepository.save(user);

        Transaction tx = Transaction.create(user, "YOUTUBE", 10450, LocalDateTime.now().minusDays(45));
        transactionRepository.save(tx);

        Subscription subscription = Subscription.createDetected(user, "YOUTUBE", 10450);
        subscriptionRepository.save(subscription);

        // when: 매일 자정에 도는 스케줄러 메서드 수동 실행
        subscriptionService.checkUnusedSubscriptions();

        // then: DB에서 다시 조회해보면 상태가 WARNING(경고)으로 바뀌어 있어야 함
        Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).get();
        assertThat(updatedSub.getStatus().name()).isEqualTo("WARNING");
    }

    @Test
    @DisplayName("E2E Scenario: 유저가 구독 액션을 수행하면, 영속성 컨텍스트(DB)에 상태 전이가 완벽하게 반영되어야 한다.")
    void test_handle_user_action() {
        // given: 갓 탐지된(DETECTED) 구독 내역 생성
        User user = User.create("액션테스트유저");
        userRepository.save(user);

        Subscription subscription = Subscription.createDetected(user, "SPOTIFY", 10900);
        subscriptionRepository.save(subscription);

        // when: 유저가 '이거 내 구독 아니야(무시할래)' 액션을 취함 (isCancelAction = false)
        subscriptionService.handleUserAction(subscription.getId(), false);

        // then: 서비스 레이어의 @Transactional에 의해 DB에도 상태가 IGNORED(무시)로 업데이트되어야 함
        Subscription updatedSub = subscriptionRepository.findById(subscription.getId()).get();
        assertThat(updatedSub.getStatus().name()).isEqualTo("IGNORED");
    }
}