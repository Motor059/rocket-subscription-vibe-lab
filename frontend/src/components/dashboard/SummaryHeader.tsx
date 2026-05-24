import React from 'react';

interface SummaryHeaderProps {
  totalAmount: number;
  activeCount: number;
}

export const SummaryHeader: React.FC<SummaryHeaderProps> = ({ totalAmount, activeCount }) => {
  return (
    <section className="bg-blue-600 rounded-2xl p-5 text-white shadow-md">
      <h2 className="text-blue-100 text-sm font-medium mb-1">이번 달 예상 구독료</h2>
      <p className="text-3xl font-bold">{totalAmount.toLocaleString()}원</p>
      <p className="text-blue-100 text-sm mt-2">총 {activeCount}개의 구독을 이용 중이에요.</p>
    </section>
  );
};