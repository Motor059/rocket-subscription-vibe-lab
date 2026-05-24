### <유스케이스 다이어그램>
```text
flowchart LR
    %% Actors
    User([청년 사용자])
    Scheduler([배치 스케줄러])
    OpenBanking([오픈뱅킹 테스트베드])

    %% System Boundary
    subgraph Rocket Subscription System [로켓 구독 시스템 (Grove)]
        UC1(결제 내역 연동 동의 및 수집)
        UC2(결제 주기성 패턴 분석 및 탐지)
        UC3(30일 미사용 구독 알림 발송)
        UC4(구독 상태 관리 - 스와이프 처리)
    end

    %% Relationships
    User -->|권한 부여| UC1
    User -->|좌/우 스와이프| UC4
    User -->|알람 확인 후 응답| UC3 
    
    OpenBanking -->|Mock 데이터 제공| UC1
    Scheduler -->|매일 자정 실행| UC2
    Scheduler -->|상태 전이 검사| UC3
```
---

### <상태 다이어그램(FSM)>
```text
stateDiagram-v2
    %% 상태 정의
    state "탐지됨 (DETECTED)" as DETECTED
    state "미사용 경고 (WARNING)" as WARNING
    state "알림 무시 (IGNORED)" as IGNORED
    state "해지 완료 (CANCELED)" as CANCELED

    %% 전이 규칙
    [*] --> DETECTED : 28~31일 간격, 동일 merchantName,
                              금액 오차 ±10% 이내 탐지 시    
    DETECTED --> WARNING : 마지막 결제일로부터 30일 초과 시 (시스템)
    DETECTED --> IGNORED : 사용자가 우측 스와이프 (구독 유지)
    
    WARNING --> CANCELED : 사용자가 좌측 스와이프 (해지 진행)
    WARNING --> IGNORED : 사용자가 우측 스와이프 (알림 무시)

    %% 종료(불변) 상태
    CANCELED --> [*] : 상태 변경 불가
    IGNORED --> [*] : 상태 변경 불가
```
---

### <클래스 다이어그램>
```text
classDiagram
    class User {
        -Long id
        -String name
        +getId() : Long
        +getName() : String
        +getSubscriptions() : List<Subscription>
        +getTransactions() : List<Transaction>
    }

    class Transaction {
        -Long id
        -String merchantName
        -int amount
        -LocalDateTime transactionDate
        +getId() : Long
        +getMerchantName() : String
        +getAmount() : int
        +getTransactionDate() : LocalDateTime
    }
    
    class Subscription {
        -Long id
        -String merchantName
        -int amount
        -SubscriptionStatus status
        +swipeRight() : void       // 구독 유지 → IGNORED
        +swipeLeft() : void        // 해지 진행 → CANCELED
        +getStatus() : SubscriptionStatus
    }

    class SubscriptionStatus {
        <<enumeration>>
        DETECTED
        WARNING
        CANCELED
        IGNORED
    }
    
    class SubscriptionDetector {
        +analyze(transactions : List<Transaction>) : List<Subscription>
    }

    class NotificationService {
        +sendAlert(userId : Long, subscription : Subscription) : void
    }

    %% 관계 정의
    User "1" *-- "0..*" Transaction : 결제 내역 보유
    User "1" o-- "0..*" Subscription : 구독 정보 보유
    Subscription --> "1" SubscriptionStatus : 상태 가짐
```
---

### <시퀀스 다이어그램>
```text
1. SD1 — 구독 자동 탐지 흐름 (Scheduler 주도)
Scheduler → TransactionRepository : getRecentTransactions(userId)
TransactionRepository → Scheduler : return transactions
Scheduler → SubscriptionDetector : analyze(transactions)
SubscriptionDetector → Subscription : new(merchantName, amount, DETECTED)
SubscriptionDetector → Scheduler : return detectedList
Scheduler → NotificationService : sendAlert(userId, detectedList)

2. SD2 — 스와이프 처리 흐름 (User 주도, alt 프레임 활용)
User → SubscriptionService : swipe(subscriptionId, direction)
alt [direction == RIGHT]
    SubscriptionService → Subscription : updateStatus(IGNORED)
else [direction == LEFT]
    SubscriptionService → Subscription : updateStatus(CANCELED)
    SubscriptionService → CancellationService : requestCancel(subscription)
end
```