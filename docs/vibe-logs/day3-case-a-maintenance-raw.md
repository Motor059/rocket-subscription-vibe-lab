# [Day 3] Case A (프로세스 미적용) 유지보수 시뮬레이션 로그

## 1. 실험 개요
- **일시:** 2026년 5월 21일
- **소모 토큰:** 누적 약 6,150 토큰 (3회차 프롬프트 핑퐁 결과)
- **특이사항:** 
  1. 동일한 대화창에서 디버깅을 지시하여 LLM의 '컨텍스트 누적(Context Accumulation)'으로 인한 토큰 폭발 현상 관찰.
  2. 요구 모델링(FSM) 없이 단순 기능 추가만 지시했을 때 발생하는 '기술 부채(Technical Debt)'와 '응집도 하락' 관찰.

## 2. 입력 프롬프트 (Prompt)
**[2회차: OOM 및 상태값 단순 수정 요구]**
"어제 네가 짜준 코드를 보니까 문제가 좀 있어.
detectSubscriptions 메서드에서 findByUserId를 쓰면 10년 치 데이터를 다 가져와서 메모리(OOM)가 터질 것 같아. 최근 90일 데이터만 가져오게 쿼리 조건을 추가해 줘. 그리고 Subscription 엔티티의 상태를 단순 boolean이 아니라 Enum(탐지됨, 경고, 해지됨, 무시됨)으로 바꿔서 다시 전체 코드를 짜줘."

**[3회차: 환각 로직 패치 요구]**
"추가적으로 txList.size() >= 2이 환각 탐지 로직으로 보이는데 대처할 방법이 없을까? 해당 코드를 수정해서 해결해줘."

## 3. AI 출력 결과 및 관찰된 기술 부채
**[치명적 결함 및 아키텍처 붕괴]**
1. FSM 전이 규칙 누락: 상태값을 Enum으로 바꾸긴 했으나, 어떤 상태에서 어떤 상태로 변해야 하는지 규칙이 없어 취소(CANCELED)된 구독도 강제로 경고(WARNING) 상태로 덮어씌우는 치명적 버그 발생.

2. 단일 책임 원칙(SRP) 위반 및 응집도 하락: 환각 로직을 고치기 위해 날짜 계산과 정렬 로직을 SubscriptionService 하나의 메서드에 모두 욱여넣어 스파게티 코드가 양산됨.

// application/SubscriptionService.java(결함이 있는 수정본 일부 발췌)
@Transactional
public void detectSubscriptions(Long userId) {
    // ... 중략 ...
    // 응집도 하락: 서비스 계층에서 날짜 정렬과 28~31일 간격 계산 로직까지 모두 처리함
    for (int i = 0; i < txList.size() - 1; i++) {
        long daysBetween = ChronoUnit.DAYS.between(current.getTransactionDate(), next.getTransactionDate());
        if (current.getAmount() == next.getAmount() && daysBetween >= 28 && daysBetween <= 31) {
            isPeriodic = true; 
        }
    }
}

@Transactional
public void checkUnusedSubscriptions() {
    // ... 중략 ...
    // FSM 규칙 누락: 현재 상태가 CANCELED 또는 IGNORED 인지 확인하지 않고 무조건 WARNING으로 변경
    if (lastTx != null && lastTx.getTransactionDate().isBefore(thirtyDaysAgo)) {
        sub.setStatus(SubscriptionStatus.WARNING);
    }
}