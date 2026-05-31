import { useState, useEffect, useCallback, useMemo } from 'react';
import type { Subscription } from '../types/subscription';

export const useSubscriptions = (userId: number) => {
  const [subscriptions, setSubscriptions] = useState<Subscription[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [isDetecting, setIsDetecting] = useState(false);

  const fetchSubscriptions = useCallback(async () => {
    setIsLoading(true);
    setError(null);
    try {
      const res = await fetch(`/api/v1/subscriptions?userId=${userId}`);
      if (!res.ok) throw new Error('구독 목록을 불러오지 못했습니다.');
      const data = await res.json();
      setSubscriptions(data);
    } catch (err) {
      setError(err instanceof Error ? err.message : '알 수 없는 오류가 발생했습니다.');
    } finally {
      setIsLoading(false);
    }
  }, [userId]);

  useEffect(() => {
    fetchSubscriptions();
  }, [fetchSubscriptions]);

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

      setSubscriptions(prev => 
        prev.map(sub => 
          sub.id === id 
            ? { ...sub, status: isCancelAction ? 'CANCELED' : 'IGNORED' } 
            : sub
        )
      );
    } catch (err) {
      throw err;
    }
  };

  const handleDetect = async () => {
    setIsDetecting(true);
    try {
      const res = await fetch(`/api/v1/subscriptions/detect?userId=${userId}`, { method: 'POST' });
      if (!res.ok) throw new Error('탐지 요청에 실패했습니다.');
      alert(`최신 결제 내역 분석이 완료되었습니다.`);
      await fetchSubscriptions();
    } catch (err) {
      alert(err instanceof Error ? err.message : '탐지 중 오류가 발생했습니다.');
    } finally {
      setIsDetecting(false);
    }
  };

  // 파생 데이터(Derived State)도 훅 내부에서 계산하여 반환
  const warningList = useMemo(() => subscriptions.filter(s => s.status === 'WARNING'), [subscriptions]);
  const activeList = useMemo(() => subscriptions.filter(s => ['DETECTED', 'CONFIRMED', 'IGNORED'].includes(s.status)), [subscriptions]);
  const totalAmount = useMemo(() => [...warningList, ...activeList].reduce((sum, s) => sum + s.amount, 0), [warningList, activeList]);

  return {
    isLoading,
    error,
    isDetecting,
    warningList,
    activeList,
    totalAmount,
    handleSubscriptionAction,
    handleDetect,
    retryFetch: fetchSubscriptions
  };
};