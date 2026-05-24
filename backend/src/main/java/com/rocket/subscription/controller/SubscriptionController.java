package com.rocket.subscription.controller;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.rocket.subscription.application.SubscriptionService;
import com.rocket.subscription.domain.Subscription;
import com.rocket.subscription.infrastructure.SubscriptionRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173") // React 개발 서버의 접근을 허용 (CORS)
public class SubscriptionController {

    private final SubscriptionService subscriptionService;
    private final SubscriptionRepository subscriptionRepository;

    // 1. 구독 목록 조회 API
    @GetMapping
    public ResponseEntity<List<SubscriptionResponse>> getSubscriptions(@RequestParam Long userId) {
        List<Subscription> subscriptions = subscriptionRepository.findByUserId(userId);
        
        // 엔티티를 DTO로 변환하여 프론트엔드 스펙에 맞춤
        List<SubscriptionResponse> response = subscriptions.stream()
                .map(sub -> new SubscriptionResponse(
                        sub.getId(), 
                        sub.getMerchantName(), 
                        sub.getAmount(), 
                        sub.getStatus().name()
                ))
                .collect(Collectors.toList());
                
        return ResponseEntity.ok(response);
    }

    // 2. 상태 변경 (해지/유지) API
    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(
            @PathVariable Long id, 
            @RequestBody ActionRequest request) {
        subscriptionService.handleUserAction(id, request.isCancelAction());
        return ResponseEntity.ok().build();
    }

    // 3. 수동 탐지 트리거 API
    @PostMapping("/detect")
    public ResponseEntity<String> detect(@RequestParam Long userId) {
        subscriptionService.detectSubscriptions(userId);
        return ResponseEntity.ok("탐지 완료");
    }

    // --- DTO 클래스 (통신용 객체) ---
    @Data
    static class SubscriptionResponse {
        private final Long id;
        private final String merchantName;
        private final int amount;
        private final String status;
    }

    @Data
    static class ActionRequest {
        @JsonProperty("isCancelAction")
        private boolean isCancelAction;
    }
}