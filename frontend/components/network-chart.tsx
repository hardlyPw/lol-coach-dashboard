"use client";

import React, { useState, useEffect, useMemo } from 'react';
import {
    LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, ReferenceLine
} from 'recharts';
import axios from 'axios';

// 1. 타입 정의
interface NetworkMetricData {
    timeIndex: number;
    count: number;
    density: number;
    cod: number;
    cid: number;
    sourceDa: number;
    targetDa: number;
    timeLabel?: string;
    realTimeSec?: number;
}

interface Pattern {
    label: string;
    source: number;
    target: number;
    color: string;
}

interface MetricOption {
    id: 'COUNT' | 'DENSITY' | 'COD' | 'CID';
    name: string;
    desc: string;
}

interface NetworkChartProps {
    matchId: number;
    timeRange?: { start: number; end: number };
    events?: any[];
    selectedPattern: string;
    onPatternChange: (newPatternLabel: string) => void;
}

// ★ 이 라벨들은 Page.tsx의 초기값과 정확히 일치해야 합니다.
const PATTERNS: Pattern[] = [
    { label: "전체 (ALL)", source: -1, target: -1, color: "#ffffff" },
    { label: "질문(Q) ➡ 답변(I)", source: 1, target: 0, color: "#8884d8" },
    { label: "지시(D) ➡ 약속(C)", source: 2, target: 3, color: "#82ca9d" },
    { label: "정보(I) ➡ 정보(I)", source: 0, target: 0, color: "#ffc658" },
    { label: "정보(I) ➡ 질문(Q)", source: 0, target: 1, color: "#ff7300" },
    { label: "정보(I) ➡ 지시(D)", source: 0, target: 2, color: "#d0ed57" },
    { label: "약속(C) ➡ 정보(I)", source: 3, target: 0, color: "#a4de6c" },
];
const METRICS: MetricOption[] = [
    { id: 'COUNT', name: '횟수 (Count)', desc: '대화 발생 총 횟수' },
    { id: 'DENSITY', name: '밀도 (Density)', desc: '연결망의 촘촘함 (0~1.0)' },
    { id: 'COD', name: '발화 독점 (Out)', desc: '주도권 (누가 말을 거는가?)' },
    { id: 'CID', name: '수신 독점 (In)', desc: '호응도 (누가 말을 듣는가?)' },
];

export default function NetworkChart({ matchId, timeRange, events = [], selectedPattern, onPatternChange }: NetworkChartProps) {
    const [data, setData] = useState<NetworkMetricData[]>([]);
    const [activeMetric, setActiveMetric] = useState<MetricOption['id']>('COUNT');

    const currentPattern = useMemo(() => {
        return PATTERNS.find(p => p.label === selectedPattern) || PATTERNS[0];
    }, [selectedPattern]);

    useEffect(() => {
        if (!matchId) return;

        const fetchData = async () => {
            try {
                const res = await axios.get<NetworkMetricData[]>(`http://3.34.82.181:8080/api/matches/${matchId}/metrics`, {
                    params: {
                        sourceDa: currentPattern.source,
                        targetDa: currentPattern.target
                    }
                });

                const formattedData = res.data.map(item => ({
                    ...item,
                    timeLabel: formatTime(item.timeIndex * 10), // 여기서 미리 포맷팅 가능하지만 아래에서 직접 처리함
                    realTimeSec: item.timeIndex * 10
                }));

                setData(formattedData);
            } catch (err) {
                console.error("데이터 로딩 실패:", err);
            }
        };

        fetchData();
    }, [matchId, currentPattern]);

    // ★ [핵심] 시간 포맷터 함수 (소수점 제거)
    const formatTime = (seconds: number): string => {
        const totalSeconds = Math.floor(Number(seconds)); // ★ 소수점 버림 (핵심!)
        const mm = Math.floor(totalSeconds / 60);
        const ss = totalSeconds % 60;
        return `${mm}:${ss.toString().padStart(2, '0')}`;
    };

    const getEventIcon = (eventName: string) => {
        const name = eventName.toLowerCase();
        if (name.includes('champion')) return '⚔️';
        if (name.includes('horde')) return '🐛';
        if (name.includes('herald')) return '👁️';
        if (name.includes('dragon')) return '🐉';
        if (name.includes('atakhan')) return '👹';
        if (name.includes('baron')) return '👾';
        if (name.includes('turret')) return '🏰';
        if (name.includes('inhib')) return '💎';
        return '🚩';
    };

    const getEventColor = (event: any) => {
        // killerId가 없으면(null) -> 흰색 (오브젝트 처형, 타워 등)
        if (event.killerId === null || event.killerId === undefined) {
            return '#ffffff';
        }

        const id = Number(event.killerId);

        // 1~5번 (또는 100번): 블루팀 -> 파란색
        if ((id >= 1 && id <= 5) || id === 100) {
            return '#3b82f6'; // Blue
        }

        // 6~10번 (또는 200번): 레드팀 -> 빨간색
        if ((id >= 6 && id <= 10) || id === 200) {
            return '#ef4444'; // Red
        }

        // 그 외(혹시 모를 예외): 흰색
        return '#ffffff';
    };

    const currentMetricDesc = METRICS.find(m => m.id === activeMetric)?.desc;

    const isZoomActive = timeRange && timeRange.end !== Infinity && timeRange.end > 0;

    const xDomain = isZoomActive
        ? [timeRange.start / 1000, timeRange.end / 1000]
        : ['dataMin', 'dataMax'];

    return (
        <div className="p-6 bg-slate-900/50 border border-slate-800 text-white rounded-xl shadow-lg h-full">
            <h2 className="text-xl font-bold mb-6 border-b border-slate-700 pb-2 flex items-center gap-2">
                🕸️ 네트워크 패턴 분석
                <span className="text-xs font-normal text-slate-400 ml-2">
        </span>
            </h2>

            <div className="flex flex-col md:flex-row justify-between items-center mb-6 gap-4">
                <div className="flex items-center gap-2">
                    <span className="text-gray-400 font-semibold text-sm">대화 패턴:</span>
                    <select
                        className="bg-slate-800 border border-slate-600 text-white rounded px-3 py-2 text-sm focus:outline-none focus:border-blue-500"
                        value={PATTERNS.findIndex(p => p.label === selectedPattern)}
                        onChange={(e) => {
                            const newIndex = Number(e.target.value);
                            const newPattern = PATTERNS[newIndex];
                            onPatternChange(newPattern.label);
                        }}
                    >
                        {PATTERNS.map((p, idx) => (
                            <option key={idx} value={idx}>{p.label}</option>
                        ))}
                    </select>
                </div>

                <div className="flex bg-slate-800 rounded-lg p-1">
                    {METRICS.map((m) => (
                        <button
                            key={m.id}
                            onClick={() => setActiveMetric(m.id)}
                            className={`px-3 py-1.5 rounded-md text-xs font-medium transition-all ${
                                activeMetric === m.id
                                    ? 'bg-blue-600 text-white shadow-md'
                                    : 'text-gray-400 hover:text-white hover:bg-slate-700'
                            }`}
                            title={m.desc}
                        >
                            {m.name}
                        </button>
                    ))}
                </div>
            </div>

            <div className="h-[350px] w-full bg-slate-900 rounded-lg p-4 border border-slate-800">
                <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={data} margin={{ top: 20, right: 30, left: 0, bottom: 0 }}>
                        <CartesianGrid strokeDasharray="3 3" stroke="#334155" opacity={0.3} vertical={false} />

                        {/* ★ [수정됨] XAxis: tickFormatter 적용 */}
                        <XAxis
                            dataKey="realTimeSec"
                            type="number"
                            domain={xDomain as any}
                            allowDataOverflow={true}
                            tickFormatter={formatTime} // formatTime 함수 연결
                            stroke="#94a3b8"
                            tick={{ fontSize: 11 }}
                            interval="preserveStartEnd"
                            tickCount={10}
                        />
                        <YAxis stroke="#94a3b8" fontSize={11} />

                        {/* ★ [수정됨] Tooltip: labelFormatter 적용 */}
                        <Tooltip
                            contentStyle={{ backgroundColor: '#1e293b', borderColor: '#334155', color: '#fff' }}
                            labelFormatter={formatTime} // formatTime 함수 연결
                        />
                        <Legend verticalAlign="top" height={36}/>

                        {events.map((event, index) => {
                            const eventSec = event.eventTime / 1000;
                            // ... (필터링 로직 유지) ...

                            return (
                                <ReferenceLine
                                    key={index}
                                    x={eventSec}
                                    // ★ 수정 포인트: 함수에 'event' 객체 통째로 넘기기
                                    stroke={getEventColor(event)}
                                    strokeDasharray="3 3"
                                    label={{
                                        position: 'insideTop',
                                        value: getEventIcon(event.eventName),
                                        fontSize: 16,
                                        fill: getEventColor(event), // ★ 아이콘/글자 색상도 같이 맞춤 (선택사항)
                                        offset: 10
                                    }}
                                />
                            );
                        })}

                        {activeMetric === 'COUNT' && (
                            <Line type="monotone" dataKey="count" name="발생 횟수" stroke="#22c55e" strokeWidth={2} dot={false} activeDot={{ r: 6 }} />
                        )}
                        {activeMetric === 'DENSITY' && (
                            <Line type="monotone" dataKey="density" name="밀도" stroke={currentPattern.color} strokeWidth={2} dot={false} activeDot={{ r: 6 }} />
                        )}
                        {activeMetric === 'COD' && (
                            <Line type="monotone" dataKey="cod" name="발화 독점도 (Out)" stroke="#f59e0b" strokeWidth={2} dot={false} activeDot={{ r: 6 }} />
                        )}
                        {activeMetric === 'CID' && (
                            <Line type="monotone" dataKey="cid" name="수신 독점도 (In)" stroke="#ec4899" strokeWidth={2} dot={false} activeDot={{ r: 6 }} />
                        )}
                    </LineChart>
                </ResponsiveContainer>
            </div>

            <div className="mt-4 text-xs text-gray-500 text-center">
                💡 {currentMetricDesc}
            </div>
        </div>
    );
}