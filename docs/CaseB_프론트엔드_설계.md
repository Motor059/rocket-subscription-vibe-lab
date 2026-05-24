# Grove — 구독 디톡스 기능 프론트엔드 설계 문서 (Case B)

> **목적**: 백엔드 API 명세, UI 상태 명세, 컴포넌트 위계 구조를 사전에 정의하여  
> AI 코드 생성 시 발생하는 결함(Case A)을 최소화한다.

---

## 1. 백엔드 API 명세

### 공통 규칙

| 항목 | 값 |
|---|---|
| Base URL | `/api/v1` |
| Content-Type | `application/json` |
| 인증 | `userId` 쿼리 파라미터로 전달 (MVP 단계) |

---

### API 1 — 구독 목록 조회

```
GET /api/v1/subscriptions?userId={userId}
```

**Response 200 (Success)**
```json
[
  {
    "id": 1,
    "merchantName": "Netflix",
    "amount": 13500,
    "status": "DETECTED",
    "lastTransactionDate": "2024-10-15T14:30:00"
  },
  {
    "id": 2,
    "merchantName": "Spotify",
    "amount": 10900,
    "status": "WARNING",
    "lastTransactionDate": "2024-09-01T09:00:00"
  }
]
```

**Response 404**
```json
{ "message": "사용자를 찾을 수 없습니다." }
```

**프론트엔드 처리 규칙**
- 응답 배열이 비어있으면(`[]`) → Empty 상태 렌더링
- `status`가 `DETECTED` 또는 `WARNING`인 항목만 렌더링
- `CANCELED`, `IGNORED`는 필터링하여 숨김

---

### API 2 — 스와이프 액션 처리

```
PATCH /api/v1/subscriptions/{id}/status
```

**Request Body**
```json
{ "isCancelAction": true }
```

**Response 200 (Success)**
```json
{ "id": 1, "status": "CANCELED" }
```

**Response 400 (FSM 위반)**
```json
{ "message": "현재 상태에서는 해당 액션을 수행할 수 없습니다." }
```

**Response 404**
```json
{ "message": "존재하지 않는 구독입니다." }
```

**프론트엔드 처리 규칙**
- 요청 중 해당 카드의 버튼 비활성화 (Pending 상태)
- 성공 시 → 목록 전체 재조회 (Refetch), 낙관적 업데이트 금지
- 400 수신 시 → 에러 토스트 메시지 표시, 버튼 복구
- 404 수신 시 → 에러 토스트 표시 후 목록 재조회

---

### API 3 — 수동 탐지 트리거

```
POST /api/v1/subscriptions/detect?userId={userId}
```

**Response 200 (Success)**
```json
{ "message": "탐지 완료", "detectedCount": 2 }
```

**프론트엔드 처리 규칙**
- 요청 중 재탐지 버튼 비활성화 + 로딩 스피너 표시
- 성공 시 → 구독 목록 자동 재조회 (Refetch)
- `detectedCount` 값을 토스트 메시지에 표시 ("2건의 구독이 탐지되었습니다")

---

## 2. UI 상태 명세

### 2-1. 구독 목록 페이지 (SubscriptionPage)

| 상태 | 진입 조건 | 렌더링 |
|---|---|---|
| **Loading** | API 요청 중 | 스켈레톤 카드 3개 |
| **Error** | 네트워크 오류 / 500 응답 | 에러 메시지 + 재시도 버튼 |
| **Empty** | 응답 배열이 `[]` | "탐지된 구독 없음" 안내 문구 |
| **Success** | 응답 배열에 데이터 존재 | 구독 카드 리스트 |

### 2-2. 구독 카드 액션 버튼 (ActionButton)

| 상태 | 진입 조건 | 렌더링 |
|---|---|---|
| **Idle** | 초기 / 요청 완료 후 | 버튼 활성화, 정상 색상 |
| **Pending** | PATCH 요청 중 | 버튼 비활성화, 로딩 스피너 |
| **Error** | 400 / 404 응답 | 버튼 복구, 에러 토스트 표시 |

### 2-3. 버튼 노출 조건 (FSM 기반)

| 구독 상태 | 해지하기 버튼 | 무시하기 버튼 |
|---|---|---|
| `DETECTED` | ❌ 숨김 | ✅ 표시 |
| `WARNING` | ✅ 표시 | ✅ 표시 |
| `CANCELED` | ❌ (목록에서 필터링됨) | ❌ |
| `IGNORED` | ❌ (목록에서 필터링됨) | ❌ |

> **근거**: 백엔드 FSM에서 `cancel()`은 WARNING 상태에서만 허용됨.  
> 프론트에서도 동일한 규칙을 UI 레벨에서 강제하여 불필요한 400 오류 방지.

---

## 3. 컴포넌트 위계 구조

```
App.tsx
└── SubscriptionPage.tsx          # 데이터 패칭, 전체 상태 소유
    ├── SubscriptionHeader.tsx    # 페이지 제목 + 재탐지 버튼
    ├── SubscriptionList.tsx      # 4대 상태(Loading/Error/Empty/Success) 분기
    │   ├── SkeletonCard.tsx      # Loading 상태 전용 스켈레톤 UI
    │   └── SubscriptionCard.tsx  # 개별 구독 카드
    │       └── ActionButton.tsx  # 버튼 Idle/Pending/Error 상태 관리
    └── Toast.tsx                 # 전역 에러/성공 알림
```

---

## 4. 컴포넌트별 책임 정의

### SubscriptionPage
- API 호출 (`fetchSubscriptions`, `handleAction`, `triggerDetection`)
- `subscriptions`, `isLoading`, `error` 상태 소유
- 모든 상태와 핸들러를 하위 컴포넌트에 props로 전달

```tsx
interface Props {} // 외부 props 없음, 페이지 루트

interface State {
  subscriptions: Subscription[];
  isLoading: boolean;
  error: string | null;
}
```

---

### SubscriptionHeader

```tsx
interface Props {
  onDetect: () => Promise<void>;  // 재탐지 트리거
  isDetecting: boolean;           // 탐지 중 여부 (버튼 비활성화용)
}
```

---

### SubscriptionList

```tsx
interface Props {
  subscriptions: Subscription[];
  isLoading: boolean;
  error: string | null;
  onAction: (id: number, isCancelAction: boolean) => Promise<void>;
  onRetry: () => void;
}
```

- `isLoading` → `SkeletonCard` 3개 렌더링
- `error` → 에러 UI 렌더링
- `subscriptions.length === 0` → Empty UI 렌더링
- 그 외 → `SubscriptionCard` 리스트 렌더링

---

### SubscriptionCard

```tsx
interface Props {
  subscription: Subscription;
  onAction: (id: number, isCancelAction: boolean) => Promise<void>;
}
```

- 카드 표시만 담당, 직접 API 호출하지 않음
- 버튼 클릭 이벤트를 부모(`SubscriptionList`)로 emit

---

### ActionButton

```tsx
interface Props {
  subscriptionId: number;
  isCancelAction: boolean;
  disabled: boolean;              // Pending 상태 시 비활성화
  isPending: boolean;             // 스피너 표시 여부
  onClick: (id: number, isCancelAction: boolean) => void;
  label: string;
  variant: 'danger' | 'neutral';
}
```

---

## 5. 데이터 타입 정의

```tsx
// 백엔드 SubscriptionStatus enum과 1:1 매핑
type SubscriptionStatus = 'DETECTED' | 'WARNING' | 'CANCELED' | 'IGNORED';

interface Subscription {
  id: number;
  merchantName: string;
  amount: number;
  status: SubscriptionStatus;
  lastTransactionDate?: string;   // ISO 8601 형식 (LocalDateTime)
}

interface ActionResponse {
  id: number;
  status: SubscriptionStatus;
}

interface DetectResponse {
  message: string;
  detectedCount: number;
}
```

---

## 6. Case A vs Case B 비교 (발표용)

| 항목 | Case A (명세 없음) | Case B (명세 있음) |
|---|---|---|
| 컴포넌트 수 | 1개 (App.tsx) | 6개 (역할 분리) |
| API 메서드 | POST (추측) | PATCH (명세 기반) |
| userId 파라미터 | 누락 | 명세에 포함 |
| Loading 처리 | 없음 | 스켈레톤 UI |
| Error 처리 | `alert()` | 상태별 UI + 토스트 |
| FSM 동기화 | 프론트가 상태 직접 변경 | 백엔드 응답 기반 Refetch |
| 버튼 노출 조건 | WARNING만 조건부 | FSM 전체 규칙 반영 |
| 날짜 파싱 | 없음 | ISO 8601 → toLocaleDateString() |
| 탐지 트리거 | 없음 | DetectButton + userId 파라미터 포함 |

---

## 7. AI 코드 생성 요청 프롬프트 (Case B용)

```
아래 설계 문서를 바탕으로 Grove 앱의 구독 디톡스 기능을 React + TypeScript로 구현해줘.

[요구사항]
1. 컴포넌트 위계 구조를 그대로 따를 것
2. 각 컴포넌트의 Props 타입을 설계 문서와 동일하게 정의할 것
3. 상태 관리는 SubscriptionPage에서만 수행하고, 하위 컴포넌트는 props만 받을 것
4. API 호출은 명세서의 엔드포인트, HTTP 메서드, 응답 형식을 정확히 따를 것
5. 4대 UI 상태(Loading/Error/Empty/Success)를 모두 구현할 것
6. FSM 조건에 따른 버튼 노출 규칙을 적용할 것
7. 성공 시 낙관적 업데이트 대신 목록 재조회(Refetch)를 사용할 것
8. 실제 API가 없으므로 fetch 호출부는 주석 처리하고 목업 데이터로 대체할 것

[설계 문서]
(이 문서 전체 첨부)
```
