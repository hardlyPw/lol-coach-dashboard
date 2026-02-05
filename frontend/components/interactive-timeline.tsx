"use client";

import { useMemo } from 'react';
import {
  ResponsiveContainer,
  ComposedChart,
  Brush,
  ReferenceLine,
  XAxis,
  YAxis, // ★ 1. YAxis 임포트 추가
} from 'recharts';

// 아이콘 매핑 함수
const getEventIcon = (name: string) => {
  if (name.includes('Kill')) return '⚔️';
  if (name.includes('Tower') || name.includes('Turret')) return '🏰';
  if (name.includes('Dragon') || name.includes('Baron')) return '🐉';
  return '📍';
};

export interface GameEvent {
  eventName: string;
  eventTime: number; // ms 단위
  killerId?: number;
  victimId?: number;
}

interface InteractiveTimelineProps {
  totalDuration: number; // 초(sec) 단위
  events: GameEvent[];
  selectedRange: { start: number; end: number }; // ms 단위
  onRangeChange: (range: { start: number; end: number }) => void;
}

export default function InteractiveTimeline({
                                              totalDuration,
                                              events,
                                              selectedRange,
                                              onRangeChange,
                                            }: InteractiveTimelineProps) {

  // 1. 차트 데이터 생성 (1초 단위)
  const chartData = useMemo(() => {
    if (!totalDuration || totalDuration <= 0) return [];
    return new Array(Math.ceil(totalDuration)).fill(0).map((_, i) => ({
      name: i,
    }));
  }, [totalDuration]);

  // 2. 시간 포맷터 (분:초)
  const formatTime = (timeInSeconds: number) => {
    const totalSec = Math.floor(timeInSeconds);
    const m = Math.floor(totalSec / 60);
    const s = totalSec % 60;
    return `${m}:${String(s).padStart(2, '0')}`;
  };

  // 3. 브러시 핸들러
  const handleBrushChange = (domain: any) => {
    if (!domain || domain.startIndex === undefined || domain.endIndex === undefined) return;

    const startMs = domain.startIndex * 1000;
    const endMs = (domain.endIndex + 1) * 1000;

    if (Math.abs(startMs - selectedRange.start) >= 1000 || Math.abs(endMs - selectedRange.end) >= 1000) {
      onRangeChange({ start: startMs, end: endMs });
    }
  };

  const currentStartIndex = Math.floor(selectedRange.start / 1000);
  const currentEndIndex = selectedRange.end === Infinity
      ? chartData.length - 1
      : Math.min(Math.ceil(selectedRange.end / 1000) - 1, chartData.length - 1);

  return (
      <div className="w-full p-0">
        <div className="h-[40px] w-full">
          <ResponsiveContainer width="100%" height="100%">
            <ComposedChart data={chartData}>

              {/* ★★★ [수정] X축과 Y축 모두 선언해야 합니다! ★★★ */}
              <XAxis dataKey="name" hide />
              <YAxis hide /> {/* 이 친구가 없어서 에러가 났던 것입니다 */}

              <Brush
                  dataKey="name"
                  height={30}
                  stroke="#3b82f6"
                  fill="#1e293b"
                  tickFormatter={formatTime}
                  onChange={handleBrushChange}
                  startIndex={Math.max(0, currentStartIndex)}
                  endIndex={Math.max(0, currentEndIndex)}
                  alwaysShowText={true}
              />


            </ComposedChart>
          </ResponsiveContainer>
        </div>
      </div>
  );
}