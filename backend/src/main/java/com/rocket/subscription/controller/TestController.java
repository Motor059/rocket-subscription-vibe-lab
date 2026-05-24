package com.rocket.subscription.controller;

import com.rocket.subscription.application.SubscriptionService;
import com.rocket.subscription.domain.Transaction;
import com.rocket.subscription.domain.User;
import com.rocket.subscription.infrastructure.TransactionRepository;
import com.rocket.subscription.infrastructure.UserRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/test")
@RequiredArgsConstructor
public class TestController {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final SubscriptionService subscriptionService;

    // 1. 가짜 결제 내역 주입 API
    @PostMapping("/seed")
    public String seedData() {
        // 유저 생성
        User user = User.create("테스트유저");
        userRepository.save(user);

        LocalDateTime now = LocalDateTime.now();

        // 넷플릭스: 70일 전, 40일 전 결제 (최근 30일 이내 결제가 없으므로 나중에 WARNING 대상)
        Transaction netflix1 = Transaction.create(user, "Netflix", 13500, now.minusDays(70));
        Transaction netflix2 = Transaction.create(user, "Netflix", 13500, now.minusDays(40));

        // 스포티파이: 30일 전, 오늘 결제 (정상 사용 중이므로 DETECTED 대상)
        Transaction spotify1 = Transaction.create(user, "Spotify", 10900, now.minusDays(30));
        Transaction spotify2 = Transaction.create(user, "Spotify", 10900, now);

        transactionRepository.saveAll(List.of(netflix1, netflix2, spotify1, spotify2));

        return "✅ 가짜 결제 데이터 세팅 완료! (유저 ID: " + user.getId() + ")";
    }

    // 2. 미사용 구독 상태(WARNING) 수동 업데이트 API (원래는 스케줄러가 매일 자정에 실행할 로직)
    @PostMapping("/check-warning")
    public String triggerWarningCheck() {
        subscriptionService.checkUnusedSubscriptions();
        return "✅ 미사용 구독 경고(WARNING) 상태 전이 완료!";
    }

    // 3. 구독 내역 추가
    @PostMapping("/transaction")
    public String addSingleTransaction(
            @RequestParam Long userId,
            @RequestBody TestTxRequest request) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("유저가 없습니다. /seed를 먼저 호출해서 유저를 생성하세요."));

        // 요청받은 'daysAgo(며칠 전)' 값을 기준으로 결제 일시 계산
        LocalDateTime txDate = LocalDateTime.now().minusDays(request.getDaysAgo());
        Transaction tx = Transaction.create(user, request.getMerchantName(), request.getAmount(), txDate);
        transactionRepository.save(tx);

        return String.format("✅ [%s] %d원 결제 내역 1건 추가 완료! (%d일 전)",
                request.getMerchantName(), request.getAmount(), request.getDaysAgo());
    }

    @Data
    static class TestTxRequest {
        private String merchantName;
        private int amount;
        private int daysAgo;
    }
}