# [Day 4] Case B (프로세스 적용) 바이브 코딩 및 토큰 절감 로그

## 1. 실험 개요
- **일시:** 2026년 5월 22일
- **소모 토큰:** 약 3,170 토큰 (단 1회 프롬프트 호출로 배포 가능 수준 도달)
- **특이사항:** AI의 컨텍스트를 초기화(New Chat)한 뒤, 인간 엔지니어가 선행 작업한 4대 설계 명세(유스케이스, FSM, 클래스, 시퀀스 다이어그램)를 프롬프트로 주입하여 토큰 절감 및 아키텍처 품질을 대조 분석함.

## 2. 입력 프롬프트 (Prompt) 요약
```text
(전략) ... 내가 직접 작성한 설계 다이어그램(유스케이스, FSM, 클래스, 시퀀스)을 기반으로 요구사항을 정리해 줄 테니, 100% 준수해서 Java 코드를 작성해.
1. OOM 방지 (SD1 반영): TransactionRepository에서 최근 90일 데이터만 조회할 것.
2. 객체 지향적 상태 관리 (FSM 반영): Subscription 엔티티 내부에 updateToWarning(), cancel(), ignore() 같은 비즈니스 메서드를 만들어서, 현재 상태가 허용할 때만 상태가 변하도록 캡슐화해.
3. 클린 아키텍처: Service 계층이 비대해지지 않도록 SubscriptionDetector와 NotificationService로 책임을 명확히 분리할 것.
```

## 3. AI 출력 결과 (성공작 - 가드레일 완벽 준수)
**[아키텍처 개선 포인트]**
1. **객체 지향적 캡슐화:** 무분별한 Setter가 사라지고 엔티티 내부에 FSM 전이 규칙이 캡슐화됨.
2. **OOM 원천 차단:** Repository 단에서 90일 기간 필터링과 시간순 정렬이 적용된 쿼리가 정확히 생성됨.
3. **단일 책임 원칙(SRP):** 시퀀스 다이어그램(SD1)에 따라 탐지 알고리즘(`SubscriptionDetector`)과 알림(`NotificationService`) 로직이 완벽하게 격리됨.

```java
// domain/Subscription.java (FSM 캡슐화 및 방어 로직 발췌)
public void updateToWarning() {
    if (this.status != SubscriptionStatus.DETECTED) {
        throw new IllegalStateException("오직 DETECTED 상태에서만 WARNING으로 전이할 수 있습니다.");
    }
    this.status = SubscriptionStatus.WARNING;
}

// application/SubscriptionService.java (SRP 기반 객체 협력 발췌)
List<Subscription> detectedList = subscriptionDetector.analyze(user, transactions);
if (!detectedList.isEmpty()) {
    subscriptionRepository.saveAll(detectedList);
    notificationService.sendAlert(userId, detectedList);
}
```

## 4. 1차시 최종 대조 분석 (A vs B 테스트 결과)

| 구분 | Case A (명세 부재) | Case B (명세 기반) |
| :--- | :--- | :--- |
| **누적 토큰 비용** | **약 6,150 토큰 초과** (디버깅 3회) | **약 3,170 토큰** (초회 1번) |
| **품질 및 상태** | 기술 부채 누적 (환각 로직, FSM 누락, God Class) | 배포 가능 (객체 지향 캡슐화, SRP 준수) |
| **결론 (Lesson Learned)** | AI에게 무작정 구현을 맡기면 컨텍스트 누적으로 유지보수 비용이 폭발함 | **초기 모델링에 토큰을 투자하면 전체 소프트웨어 생명주기 비용을 50% 이상 아낄 수 있음** |