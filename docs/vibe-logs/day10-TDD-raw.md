# SubscriptionDetector TDD 트러블슈팅 및 엣지 케이스 검증 로그

---

## Phase 1. 가맹점명 파편화 및 빈 리스트 방어
**커밋 메시지:** `test: SubscriptionDetector 1차 엣지 케이스 테스트` 

### 🔴 RED (테스트 실패)
* **상황:** 가맹점명 파편화 및 빈 리스트 주입 실패 테스트 작성.
```text
> Task :test
SubscriptionDetectorTest > Scenario 4: 가맹점명에 대소문자/공백 오타가 있어도 하나의 구독으로 통합 탐지해야 한다. FAILED
    java.lang.AssertionError at SubscriptionDetectorTest.java:49
> Task :test FAILED
BUILD FAILED in 41s
```
### 🟢 GREEN (테스트 성공)
* **해결**: groupingBy 로직에 대소문자/공백 정규화 적용하여 테스트 통과.
```text
BUILD SUCCESSFUL in 16s
```

## Phase 2. 금액 오차 허용 클러스터링 알고리즘 적용
**커밋 메시지**: `test: SubscriptionDetector 2차 금액 오차 허용 클러스터링 알고리즘 적용 테스트`   

### 🔴 RED 테스트 실패
* **상황**: 결제 금액 인상(10,000원 -> 10,500원) 시 정기구독 탐지 누락 테스트 작성.  

```text
> Task :test
SubscriptionDetectorTest > Scenario 2: 결제 금액이 ±10% 이내로 변경되어도 하나의 정기 구독으로 묶여야 한다. FAILED
    java.lang.AssertionError at SubscriptionDetectorTest.java:71
> Task :test FAILED
BUILD FAILED in 15s
```

### 🟢 GREEN 테스트 성공
* **해결**: groupingBy 대신 오차율 기반 클러스터링 로직 구현하여 테스트 통과.  
```text
PlaintextBUILD SUCCESSFUL in 20s 
```

## Phase 3. 90일 경계값 및 최소 주기 방어 로직
**커밋 메시지**: `test: SubscriptionDetector 3차 90일 경계값 및 최소 주기 방어 로직 테스트`

### 🔴 RED 테스트 실패
* **상황**: 91일 전 결제 및 24일 간격 결제 엣지 케이스 테스트.  

```text
> Task :test
SubscriptionDetectorTest > Scenario 1: 90일이 지난 과거의 결제 내역은 정기구독 탐지 대상에서 제외되어야 한다. FAILED
    java.lang.AssertionError at SubscriptionDetectorTest.java:93
> Task :test FAILED
BUILD FAILED in 16s
```

### 🟢 GREEN 테스트 성공
* **해결**: 90일 이전 데이터 필터링 로직 추가 및 조기 종료 최적화.  
```text
PlaintextBUILD SUCCESSFUL in 16s 
```

## Phase 4. FSM 결함 수정 및 취약점 보완
**커밋 메시지**: `refactor: AI가 누락한 SubscriptionStatus 복구 및 FSM 결함 수정`

### REFACTOR
* **상황**: 겉으로는 빌드 성공이 떴지만, 코드 분석 후 cancel 메서드의 취약점 발견.

* **해결**: AI 바이브 코딩으로 누락되었던 CONFIRMED 상태 Enum을 명시적으로 추가하고, cancel() 메서드의 억지 논리를 실제 비즈니스 도메인 규칙에 맞게 리팩토링 완료.