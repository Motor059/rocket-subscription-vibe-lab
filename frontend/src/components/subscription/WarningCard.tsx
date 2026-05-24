import React, { useState } from 'react';
import type { Subscription } from '../../types/subscription'; // ✅ 수정: .ts 확장자 제거

interface WarningCardProps {
  subscription: Subscription;
  onAction: (id: number, isCancelAction: boolean) => Promise<void>;
}

export const WarningCard: React.FC<WarningCardProps> = ({ subscription, onAction }) => {
  const [isProcessing, setIsProcessing] = useState(false);
  const [actionError, setActionError] = useState<string | null>(null);

  const handleAction = async (isCancel: boolean) => {
    setIsProcessing(true);
    setActionError(null);
    try {
      await onAction(subscription.id, isCancel);
    } catch (err) {
      setActionError(err instanceof Error ? err.message : '처리 중 오류가 발생했습니다.');
    } finally {
      setIsProcessing(false);
    }
  };

  const formattedDate = subscription.lastTransactionDate
    ? new Date(subscription.lastTransactionDate).toLocaleDateString('ko-KR', {
        year: 'numeric', month: 'long', day: 'numeric',
      })
    : null;

  return (
    <div className="bg-red-50 border border-red-200 rounded-xl p-4 shadow-sm">
      <div className="flex justify-between items-start mb-3">
        <div>
          <span className="inline-flex items-center px-2 py-0.5 rounded text-xs font-semibold bg-red-100 text-red-800 mb-2">
            미사용 의심
          </span>
          <h3 className="text-lg font-bold text-gray-900">{subscription.merchantName}</h3>
          <p className="text-sm text-gray-600">40일 이상 결제 이력이 없습니다.</p>
          {formattedDate && (
            <p className="text-xs text-red-500 mt-1">마지막 결제: {formattedDate}</p>
          )}
        </div>
        <span className="text-lg font-bold text-red-600">
          {subscription.amount.toLocaleString()}원
        </span>
      </div>

      {actionError && (
        <p className="text-xs text-red-600 bg-red-100 px-3 py-1.5 rounded-lg mb-3">
          {actionError}
        </p>
      )}

      <div className="flex gap-2 mt-2">
        <button
          onClick={() => handleAction(true)}
          disabled={isProcessing}
          className="flex-1 bg-red-500 hover:bg-red-600 text-white py-2.5 rounded-lg
                     font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {isProcessing && (
            <span className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
          )}
          해지하기
        </button>
        <button
          onClick={() => handleAction(false)}
          disabled={isProcessing}
          className="flex-1 bg-white hover:bg-gray-50 text-gray-700 border border-gray-300
                     py-2.5 rounded-lg font-medium transition-colors disabled:opacity-50 flex items-center justify-center gap-2"
        >
          {isProcessing && (
            <span className="w-4 h-4 border-2 border-gray-400 border-t-transparent rounded-full animate-spin" />
          )}
          유지하기
        </button>
      </div>
    </div>
  );
};