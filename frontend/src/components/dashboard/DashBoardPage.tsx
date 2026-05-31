import React from 'react';
import { MobileLayout } from '../layout/MobileLayout';
import { WarningCard } from '../subscription/WarningCard';
import { ActiveCard } from '../subscription/ActiveCard';
import { EmptyState } from '../subscription/EmptyState';
import { Spinner } from '../common/Spinner';
import { SummaryHeader } from './SummaryHeader';
import { useSubscriptions } from '../../hooks/useSubscription';

const MOCK_USER_ID = 1;

export const DashBoardPage: React.FC = () => {
  // 복잡한 상태와 함수들을 커스텀 훅에서 단 한 줄로 깔끔하게 가져옵니다!
  const {
    isLoading, error, isDetecting, 
    warningList, activeList, totalAmount, 
    handleSubscriptionAction, handleDetect, retryFetch
  } = useSubscriptions(MOCK_USER_ID);

  if (isLoading) {
    return (
      <MobileLayout title="구독 관리" onDetect={handleDetect} isDetecting={isDetecting}>
        <Spinner />
      </MobileLayout>
    );
  }

  if (error) {
    return (
      <MobileLayout title="구독 관리" onDetect={handleDetect} isDetecting={isDetecting}>
        <div className="flex flex-col items-center justify-center h-64 gap-4 text-center">
          <div className="w-14 h-14 rounded-full bg-red-100 flex items-center justify-center text-2xl">⚠️</div>
          <div>
            <p className="font-bold text-gray-900 mb-1">오류가 발생했습니다</p>
            <p className="text-sm text-gray-500">{error}</p>
          </div>
          <button onClick={retryFetch} className="px-6 py-2 bg-blue-600 text-white rounded-lg font-medium">
            다시 시도
          </button>
        </div>
      </MobileLayout>
    );
  }

  return (
    <MobileLayout title="구독 관리" onDetect={handleDetect} isDetecting={isDetecting}>
      <SummaryHeader totalAmount={totalAmount} activeCount={activeList.length + warningList.length} />

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

      <section>
        <h2 className="text-lg font-bold text-gray-800 mb-3">이용 중인 구독</h2>
        {activeList.length === 0 ? <EmptyState /> : (
          <div className="space-y-3">
            {activeList.map(sub => <ActiveCard key={sub.id} subscription={sub} />)}
          </div>
        )}
      </section>
    </MobileLayout>
  );
};