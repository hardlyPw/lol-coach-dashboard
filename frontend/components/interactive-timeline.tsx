"use client";

import { useMemo } from 'react';
import { ResponsiveContainer, ComposedChart, Brush, XAxis, YAxis } from 'recharts';

export default function InteractiveTimeline({ totalDuration, selectedRange, onRangeChange }: any) {

  // 차트의 바닥 데이터를 만듭니다 (선택 슬라이더용)
  const chartData = useMemo(() => {
    if (!totalDuration || totalDuration <= 0) return [];
    return new Array(Math.ceil(totalDuration)).fill(0).map((_, i) => ({ name: i }));
  }, [totalDuration]);

  const currentStartIndex = Math.max(0, Math.floor(selectedRange.start / 1000));
  const currentEndIndex = selectedRange.end === Infinity || isNaN(selectedRange.end)
      ? chartData.length - 1
      : Math.min(Math.floor(selectedRange.end / 1000), chartData.length - 1);

  return (
      <div className="w-full p-0">
        <div className="h-[40px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData}>
              <XAxis dataKey="name" hide />
              <YAxis hide />

              {/* ★ 이벤트를 그리던 ReferenceLine들을 싹 지웠습니다. 훨씬 깔끔하죠? */}

              <Brush
                  dataKey="name"
                  height={30}
                  stroke="#3b82f6"
                  fill="#1e293b"
                  tickFormatter={(val) => {
                    const m = Math.floor(val / 60);
                    const s = val % 60;
                    return `${m}:${String(s).padStart(2, '0')}`;
                  }}
                  startIndex={currentStartIndex}
                  endIndex={currentEndIndex}
                  onChange={(domain: any) => {
                    if (!domain || domain.startIndex === undefined) return;
                    onRangeChange({ start: domain.startIndex * 1000, end: domain.endIndex * 1000 });
                  }}
              />
            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>
  );
}