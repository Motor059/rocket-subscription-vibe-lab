# [Day 2] Case A (프로세스 미적용) 바이브 코딩 로그

## 1. 실험 개요
- **일시:** 2026년 5월 20일
- **소모 토큰:** 약 2,040 토큰
- **특이사항:** AI에게 요구사항 명세서 없이 구현을 지시함.

## 2. 입력 프롬프트 (Prompt)
```
Spring Boot와 JPA를 이용해서 사용자의 카드 결제 내역을 분석해 '미사용 구독 서비스'를 찾아내는 앱을 만들려고 해.
User, Transaction(결제 내역), Subscription(탐지된 구독) 엔티티 클래스를 알아서 짜줘.
그리고 Service 클래스에 Transaction 내역들을 분석해서 매월 정기 결제되는 걸 Subscription으로 저장하고, 30일 동안 안 쓴 건 미사용이라고 표시하는 핵심 로직도 같이 짜줘.
```

## 3. AI 출력 결과
**[치명적 결함]**
1. OOM 유발: 쿼리 단에서 날짜 필터링 없이 10년치 데이터를 모두 메모리로 가져옴.
2. 상태 기계 누락: Subscription의 상태를 FSM이 아닌 `boolean`으로 단편적으로 처리함.
3. 환각 탐지: 결제 주기(30일) 확인 없이 단순히 2번 결제하면 구독으로 간주함.

```java
// domain/Subscription.java
@Entity
@Getter @Setter
public class Subscription {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;
    private String merchantName;
    private int amount;
    private boolean isUnused; // FSM 누락 (단순 boolean)
}

// application/SubscriptionService.java (일부 발췌)
@Transactional
public void detectSubscriptions(Long userId) {
    // OOM 위험: 기간 조건 누락
    List<Transaction> transactions = transactionRepository.findByUserId(userId);
    // ... 중략 ...
    // 환각 논리: 단순 2회 이상 결제로 판정
    if (txList.size() >= 2) {
        Subscription sub = new Subscription();
        // ...
        subscriptionRepository.save(sub);
    }
}
```