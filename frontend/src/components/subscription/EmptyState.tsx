import React from 'react';

export const EmptyState: React.FC = () => {
  return (
    <div className="text-center py-12 bg-gray-50 rounded-xl border border-gray-200">
      <div className="w-16 h-16 rounded-full bg-gray-100 flex items-center justify-center mx-auto mb-4 text-2xl">
        ✅
      </div>
      <p className="font-bold text-gray-700 mb-1">탐지된 구독이 없습니다</p>
      <p className="text-sm text-gray-400">모든 결제 내역이 정상 관리되고 있습니다.</p>
    </div>
  );
};