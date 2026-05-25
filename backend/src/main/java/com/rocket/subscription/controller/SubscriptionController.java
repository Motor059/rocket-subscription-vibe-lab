package com.rocket.subscription.controller;

import com.rocket.subscription.application.SubscriptionService;
import com.rocket.subscription.application.dto.SubscriptionActionRequest;
import com.rocket.subscription.application.dto.SubscriptionResponse;
import com.rocket.subscription.domain.Subscription;
import com.rocket.subscription.infrastructure.SubscriptionRepository;
import com.rocket.subscription.infrastructure.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;
    private final TransactionRepository transactionRepository;

    // 1. 구독 목록 조회 API
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(@RequestParam Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);

        // 엔티티를 DTO로 변환 + 가장 최근 결제일(lastTransactionDate) 조합
        List<SubscriptionResponse> response = subscriptions.stream()
                .map(sub -> {
                    var lastTx = transactionRepository.findTopByUserIdAndMerchantNameOrderByTransactionDateDesc(
                            userId, sub.getMerchantName());
                    var lastTxDate = (lastTx != null) ? lastTx.getTransactionDate() : null;

                    return SubscriptionResponse.of(sub, lastTxDate);
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // 2. 상태 변경 (해지/유지) API
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id,
            @RequestBody SubscriptionActionRequest request) {
        try {
            subscriptionService.handleUserAction(id, request.isCancelAction());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            // FSM 규칙 위반 시 400 Bad Request 반환 -> 프론트엔드의 Error State 유도
            return ResponseEntity.badRequest().build();
        }
    }

    // 3. 수동 탐지 트리거 API
    @PostMapping("/detect")
    public ResponseEntity<String> detect(@RequestParam Long userId) {
        subscriptionService.detectSubscriptions(userId);
        return ResponseEntity.ok("탐지 완료");
    }
}