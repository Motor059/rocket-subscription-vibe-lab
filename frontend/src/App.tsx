import React, { useState, useEffect } from 'react';
import { AlertCircle, CheckCircle2, CreditCard, XCircle, BellOff, Loader2, RefreshCw } from 'lucide-react';

const STATUS = {
  DETECTED: 'DETECTED',
  WARNING: 'WARNING',
  CANCELED: 'CANCELED',
  IGNORED: 'IGNORED',
};

// 백엔드 API 명세서 구조와 동일하게 맞춤
interface Subscription {
  id: number;
  merchantName: string;
  amount: number;
  status: string;
  lastTransactionDate?: string;
}

export default function SubscriptionDashboard() {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  // 4대 상태 관리를 위한 State 추가
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // 1. 목록 조회 로직 (로딩/에러 처리 포함)
  const fetchSubscriptions = async () => {
    setIsLoading(true);
    setError(null);
    try {
      // API 명세서 동기화
      const response = await fetch('/api/v1/subscriptions/detected');
      if (!response.ok) throw new Error('구독 목록을 불러오지 못했습니다.');
      
      const data = await response.json();
      setSubscriptions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  };

  useEffect(() => {
    // 실제 API가 없으므로 1초 뒤 임시 목업 데이터 반환으로 시뮬레이션
    const mockDelay = setTimeout(() => {
      setSubscriptions([
        { id: 1, merchantName: 'Netflix', amount: 13500, status: STATUS.DETECTED, lastTransactionDate: '2023-10-15T14:30:00' },
        { id: 2, merchantName: 'Spotify', amount: 10900, status: STATUS.WARNING, lastTransactionDate: '2023-09-01T09:00:00' },
      ]);
      setIsLoading(false);
    }, 1000);
    return () => clearTimeout(mockDelay);
    // fetchSubscriptions(); // 실제 연동 시 주석 해제
  }, []);

  // 2. 상태 액션 처리 로직 (FSM 보호 및 PATCH 동기화)
  const handleAction = async (id: number, isCancelAction: boolean) => {
    try {
      // 프론트에서 상태를 미리 덮어씌우지 않고, 백엔드의 성공 응답을 기다림
      /* 실제 연동 로직
      const response = await fetch(`/api/v1/subscriptions/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isCancelAction }),
      });
      if (!response.ok) {
        if(response.status === 400) throw new Error('현재 상태에서는 해당 액션을 수행할 수 없습니다.');
        throw new Error('상태 변경 실패');
      }
      */
      
      // 낙관적 업데이트(Optimistic Update) 대신, 성공 시 목록 리패치(Refetch)로 백엔드와 완벽 동기화
      alert(`${isCancelAction ? '해지' : '무시'} 요청이 백엔드에 전달되었습니다.`);
      // fetchSubscriptions();
      
      // 임시 UI 갱신 (목업용)
      setSubscriptions(prev => prev.filter(sub => sub.id !== id));
      
    } catch (err) {
      alert(err instanceof Error ? err.message : '처리 중 오류가 발생했습니다.');
    }
  };

  // 3. 수동 탐지 트리거 로직 추가
  const triggerDetection = async () => {
    try {
      alert('구독 탐지 배치를 백그라운드에서 실행합니다.');
      // await fetch('/api/v1/subscriptions/detect', { method: 'POST' });
    } catch (err) {
      console.error(err);
    }
  };

  const activeSubscriptions = subscriptions.filter(
    (sub) => sub.status === STATUS.DETECTED || sub.status === STATUS.WARNING
  );

  // === UI 렌더링 부 ===

  if (isLoading) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center">
        <div className="flex flex-col items-center gap-2 text-gray-500">
          <Loader2 className="w-8 h-8 animate-spin text-blue-600" />
          <p>구독 내역을 분석하고 있습니다...</p>
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <div className="min-h-screen bg-gray-50 p-6 flex items-center justify-center">
        <div className="text-center">
          <AlertCircle className="w-12 h-12 text-red-500 mx-auto mb-4" />
          <h2 className="text-xl font-bold text-gray-900 mb-2">오류가 발생했습니다</h2>
          <p className="text-gray-500 mb-6">{error}</p>
          <button 
            onClick={fetchSubscriptions}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition"
          >
            다시 시도
          </button>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-gray-50 p-6 flex justify-center">
      <div className="w-full max-w-2xl">
        <header className="mb-8 flex justify-between items-end">
          <div>
            <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-2">
              <CreditCard className="text-blue-600" />
              내 구독 관리
            </h1>
            <p className="text-gray-500 mt-1">자동으로 탐지된 정기 결제 내역입니다.</p>
          </div>
          
          <button 
            onClick={triggerDetection}
            className="flex items-center gap-1.5 text-sm font-medium text-blue-600 bg-blue-50 px-3 py-1.5 rounded-lg hover:bg-blue-100 transition"
          >
            <RefreshCw className="w-4 h-4" />
            재탐지
          </button>
        </header>

        <div className="space-y-4">
          {activeSubscriptions.length === 0 ? (
            <div className="bg-white p-12 rounded-xl shadow-sm text-center border border-gray-200">
              <div className="bg-gray-100 w-16 h-16 rounded-full flex items-center justify-center mx-auto mb-4">
                <CreditCard className="w-8 h-8 text-gray-400" />
              </div>
              <h3 className="text-lg font-bold text-gray-900 mb-1">탐지된 구독이 없습니다</h3>
              <p className="text-gray-500">모든 결제 내역이 정상적으로 관리되고 있습니다.</p>
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
                    {sub.status === STATUS.WARNING && sub.lastTransactionDate && (
                      <p className="text-sm text-red-600 mt-2 font-medium bg-red-100 inline-block px-2 py-0.5 rounded">
                        마지막 사용일로부터 30일이 지났어요! (결제: {new Date(sub.lastTransactionDate).toLocaleDateString()})
                      </p>
                    )}
                  </div>
                </div>

                <div className="flex gap-2 w-full md:w-auto">
                  {/* FSM 조건: 해지 버튼은 WARNING 상태에서만 노출 */}
                  {sub.status === STATUS.WARNING && (
                    <button
                      onClick={() => handleAction(sub.id, true)}
                      className="flex-1 md:flex-none flex items-center justify-center gap-1.5 px-4 py-2 bg-red-600 hover:bg-red-700 text-white text-sm font-medium rounded-lg transition-colors"
                    >
                      <XCircle className="w-4 h-4" />
                      해지하기
                    </button>
                  )}
                  {/* 무시 버튼은 DETECTED, WARNING 모두 노출 가능 (FSM 설계 기반) */}
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