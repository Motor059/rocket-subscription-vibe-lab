import React, { useState, useEffect, useCallback } from 'react';
import { MobileLayout } from '../layout/MobileLayout';
import { WarningCard } from '../subscription/WarningCard';
import { ActiveCard } from '../subscription/ActiveCard';
import { EmptyState } from '../subscription/EmptyState';
import { Spinner } from '../common/Spinner';
import { SummaryHeader } from './SummaryHeader';
import type { Subscription } from '../../types/subscription';

const MOCK_USER_ID = 1;

export const DashBoardPage: React.FC = () => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);

  // ─── 목록 조회 (Refetch 기반) ────────────────────────────
  const fetchSubscriptions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/v1/subscriptions?userId=${MOCK_USER_ID}`);
      if (!res.ok) throw new Error('구독 목록을 불러오지 못했습니다.');
      const data = await res.json();
      setSubscriptions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, []);

  useEffect(() => {
    fetchSubscriptions();
  }, [fetchSubscriptions]);

  // ─── 스와이프 액션 처리 (낙관적 업데이트 적용) ───────────────
  const handleSubscriptionAction = async (id: number, isCancelAction: boolean) => {
    try {
      const res = await fetch(`/api/v1/subscriptions/${id}/status`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ isCancelAction }),
      });
      if (!res.ok) {
        if (res.status === 400) throw new Error('현재 상태에서는 해당 액션을 수행할 수 없습니다.');
        throw new Error('상태 변경에 실패했습니다.');
      }

      // UI 즉시 반영 (낙관적 업데이트)
      setSubscriptions(prev => 
        prev.map(sub => {
          if (sub.id === id) {
            // 해지하기(true)를 눌렀으면 'CANCELED', 유지하기(false)를 눌렀으면 'IGNORED'로 변경
            return { ...sub, status: isCancelAction ? 'CANCELED' : 'IGNORED' };
          }
          return sub;
        })
      );
    } catch (err) {
      throw err;
    }
  };

  // ─── 수동 탐지 트리거 ────────────────────────────────────
  const handleDetect = async () => {
    setIsDetecting(true);
    try {
      const res = await fetch(`/api/v1/subscriptions/detect?userId=${MOCK_USER_ID}`, {
        method: 'POST',
      });
      if (!res.ok) throw new Error('탐지 요청에 실패했습니다.');
      alert(`최신 결제 내역 분석이 완료되었습니다.`);
      await fetchSubscriptions();
    } catch (err) {
      alert(err instanceof Error ? err.message : '탐지 중 오류가 발생했습니다.');
    } finally {
      setIsDetecting(false);
    }
  };

  // ─── 파생 데이터 ─────────────────────────────────────────
  const warningList = subscriptions.filter(s => s.status === 'WARNING');
  const activeList  = subscriptions.filter(s => s.status === 'DETECTED' || s.status === 'IGNORED');  const totalAmount = [...warningList, ...activeList].reduce((sum, s) => sum + s.amount, 0);

  // ─── 렌더링 분기 ─────────────────────────────────────────

  // Loading 상태
  if (isLoading) {
    return (
      <MobileLayout title="구독 관리" onDetect={handleDetect} isDetecting={isDetecting}>
        <Spinner />
      </MobileLayout>
    );
  }

  // Error 상태
  if (error) {
    return (
      <MobileLayout title="구독 관리" onDetect={handleDetect} isDetecting={isDetecting}>
        <div className="flex flex-col items-center justify-center h-64 gap-4 text-center">
          <div className="w-14 h-14 rounded-full bg-red-100 flex items-center justify-center text-2xl">⚠️</div>
          <div>
            <p className="font-bold text-gray-900 mb-1">오류가 발생했습니다</p>
            <p className="text-sm text-gray-500">{error}</p>
          </div>
          <button
            onClick={fetchSubscriptions}
            className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium hover:bg-blue-700 transition"
          >
            다시 시도
          </button>
        </div>
      </MobileLayout>
    );
  }

  // Success / Empty 상태
  return (
    <MobileLayout title="구독 관리" onDetect={handleDetect} isDetecting={isDetecting}>

      <SummaryHeader
        totalAmount={totalAmount}
        activeCount={activeList.length + warningList.length}
      />

      {/* WARNING 섹션 */}
      {warningList.length > 0 && (
        <section>
          <h2 className="text-lg font-bold text-gray-800 mb-3">안 쓰는 구독이 있어요 🚨</h2>
          <div className="space-y-3">
            {warningList.map(sub => (
              <WarningCard key={sub.id} subscription={sub} onAction={handleSubscriptionAction} />
            ))}
          </div>
        </section>
      )}

      {/* DETECTED 섹션 */}
      <section>
        <h2 className="text-lg font-bold text-gray-800 mb-3">이용 중인 구독</h2>
        {activeList.length === 0
          ? <EmptyState />
          : (
            <div className="space-y-3">
              {activeList.map(sub => (
                <ActiveCard key={sub.id} subscription={sub} />
              ))}
            </div>
          )
        }
      </section>

    </MobileLayout>
  );
};