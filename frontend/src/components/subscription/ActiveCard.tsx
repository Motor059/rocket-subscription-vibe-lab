import React from 'react';
import type { Subscription } from '../../types/subscription';

interface ActiveCardProps {
  subscription: Subscription;
}

export const ActiveCard: React.FC<ActiveCardProps> = ({ subscription }) => {
  return (
    <div className="bg-white border border-gray-200 rounded-xl p-4 shadow-sm flex justify-between items-center">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-full bg-blue-100 flex items-center justify-center
                        text-blue-600 font-bold text-lg">
          {subscription.merchantName.charAt(0)}
        </div>
        <div>
          <h3 className="font-semibold text-gray-900">{subscription.merchantName}</h3>
          <p className="text-xs text-gray-500">정기 결제 탐지됨</p>
        </div>
      </div>
      <span className="font-bold text-gray-900">
        {subscription.amount.toLocaleString()}원
        <span className="text-xs text-gray-400 font-normal"> /월</span>
      </span>
    </div>
  );
};