import React, { useState } from 'react';
import { AlertCircle, CheckCircle2, CreditCard, XCircle, BellOff } from 'lucide-react';

// 백엔드 Enum과 매칭되는 상태
const STATUS = {
  DETECTED: 'DETECTED',
  WARNING: 'WARNING',
  CANCELED: 'CANCELED',
  IGNORED: 'IGNORED',
};

// 백엔드 API에서 받아왔다고 가정한 목업 데이터
const initialSubscriptions = [
  { id: 1, merchantName: 'Netflix', amount: 13500, status: STATUS.DETECTED, lastPaid: '2023-10-15' },
  { id: 2, merchantName: 'Spotify', amount: 10900, status: STATUS.WARNING, lastPaid: '2023-09-01' }, // 30일 이상 미사용 의심
  { id: 3, merchantName: 'Adobe Creative Cloud', amount: 62000, status: STATUS.DETECTED, lastPaid: '2023-10-10' },
];

export default function SubscriptionDashboard() {
  const [subscriptions, setSubscriptions] = useState(initialSubscriptions);

  // 백엔드의 handleUserAction API 호출을 담당할 함수
  const handleAction = async (id, isCancelAction) => {
    try {
      // TODO: 백엔드 API 연동 (POST /api/subscriptions/{id}/action)
      // await axios.post(`/api/subscriptions/${id}/action`, { isCancelAction });
      
      setSubscriptions((prev) =>
        prev.map((sub) => {
          if (sub.id === id) {
            return {
              ...sub,
              status: isCancelAction ? STATUS.CANCELED : STATUS.IGNORED,
            };
          }
          return sub;
        })
      );
    } catch (error) {
      alert('상태 변경 중 오류가 발생했습니다.');
    }
  };

  // 활성화된 구독(탐지 + 경고)만 필터링
  const activeSubscriptions = subscriptions.filter(
    (sub) => sub.status === STATUS.DETECTED || sub.status === STATUS.WARNING
  );

  return (
    <div className="min-h-screen bg-gray-50 p-6 flex justify-center">
      <div className="w-full max-w-2xl">
        <header className="mb-8">
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
            <CreditCard className="text-blue-600" />
            내 구독 관리
          </h1>
          <p className="text-gray-500 mt-1">자동으로 탐지된 정기 결제 내역입니다.</p>
        </header>

        <div className="space-y-4">
          {activeSubscriptions.length === 0 ? (
            <div className="bg-white p-8 rounded-xl shadow-sm text-center text-gray-500 border border-gray-100">
              활성화된 구독 내역이 없습니다.
            </div>
          ) : (
            activeSubscriptions.map((sub) => (
              <div 
                key={sub.id} 
                className={`p-5 rounded-xl border flex flex-col md:flex-row md:items-center justify-between gap-4 transition-all ${
                  sub.status === STATUS.WARNING 
                    ? 'bg-red-50 border-red-200' 
                    : 'bg-white border-gray-200 shadow-sm'
                }`}
              >
                <div className="flex items-start gap-4">
                  <div className="mt-1">
                    {sub.status === STATUS.WARNING ? (
                      <AlertCircle className="text-red-500 w-6 h-6" />
                    ) : (
                      <CheckCircle2 className="text-green-500 w-6 h-6" />
                    )}
                  </div>
                  
                  <div>
                    <h3 className="font-semibold text-gray-900 text-lg">
                      {sub.merchantName}
                    </h3>
                    <div className="flex items-center gap-2 mt-1">
                      <span className="text-gray-900 font-medium">
                        {sub.amount.toLocaleString()}원 <span className="text-gray-500 text-sm font-normal">/ 월</span>
                      </span>
                    </div>
                    {sub.status === STATUS.WARNING && (
                      <p className="text-sm text-red-600 mt-2 font-medium bg-red-100 inline-block px-2 py-0.5 rounded">
                        마지막 사용일로부터 30일이 지났어요! (최근 결제: {sub.lastPaid})
                      </p>
                    )}
                  </div>
                </div>

                {/* 우측 액션 버튼들 (isCancelAction 파라미터 제어) */}
                <div className="flex gap-2 w-full md:w-auto">
                  {sub.status === STATUS.WARNING && (
                    <button
                      onClick={() => handleAction(sub.id, true)}
                      className="flex-1 md:flex-none flex items-center justify-center gap-1.5 px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors"
                    >
                      <XCircle className="w-4 h-4" />
                      해지하기
                    </button>
                  )}
                  <button
                    onClick={() => handleAction(sub.id, false)}
                    className="flex-1 md:flex-none flex items-center justify-center gap-1.5 px-4 py-2 bg-gray-100 hover:bg-gray-200 text-gray-700 text-sm font-medium rounded-lg transition-colors"
                  >
                    <BellOff className="w-4 h-4" />
                    무시하기
                  </button>
                </div>
              </div>
            ))
          )}
        </div>
      </div>
    </div>
  );
}