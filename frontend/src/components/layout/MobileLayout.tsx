import React from 'react';

interface MobileLayoutProps {
  children: React.ReactNode;
  title: string;
  onDetect: () => void;
  isDetecting: boolean;
}

export const MobileLayout: React.FC<MobileLayoutProps> = ({
  children,
  title,
  onDetect,
  isDetecting,
}) => {
  return (
    <div className="min-h-screen bg-gray-100 flex justify-center">
      <div className="w-full max-w-md bg-white min-h-screen shadow-lg flex flex-col">

        {/* ✅ 수정: 재탐지 버튼이 헤더에 포함 */}
        <header className="sticky top-0 bg-white z-10 px-5 py-4 border-b border-gray-200 flex items-center justify-between">
          <h1 className="text-xl font-bold text-gray-800">{title}</h1>
          <button
            onClick={onDetect}
            disabled={isDetecting}
            className="flex items-center gap-1.5 text-sm font-medium text-blue-600 bg-blue-50
                       px-3 py-1.5 rounded-lg hover:bg-blue-100 transition disabled:opacity-50"
          >
            {/* 탐지 중일 때 스피너 표시 */}
            {isDetecting
              ? <span className="w-4 h-4 border-2 border-blue-500 border-t-transparent rounded-full animate-spin inline-block" />
              : <span>🔄</span>
            }
            재탐지
          </button>
        </header>

        <main className="flex-1 overflow-y-auto p-5 pb-10 space-y-6">
          {children}
        </main>
      </div>
    </div>
  );
};