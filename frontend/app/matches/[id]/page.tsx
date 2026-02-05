'use client'

import { useState, useEffect, useMemo } from 'react'
import axios from 'axios'

// 컴포넌트 import (경로는 본인 프로젝트에 맞게 확인해주세요)
import InteractiveTimeline from '@/components/interactive-timeline';
import { CommunicationLog } from '@/components/communication-log'
import { DensityMetrics } from '@/components/density-metrics'
import { CentralizationRadar } from '@/components/centralization-radar'
import UploadModal from '@/components/upload-modal'
import NetworkChart from '@/components/network-chart'
import {useParams, useRouter} from 'next/navigation' // ★ 1. 이 녀석을 꼭 임포트하세요!
import MatchTabs from '@/components/match-tabs';
// 데이터 타입 정의
interface NetworkMetric {
    timeIndex: number;
    realTimeSec: number;
    count: number;
    density: number;
    positionDaCounts: string;
    positionReceiveCounts: string;
}

// 문자열 파싱 함수
const parsePositionCounts = (str: string | null) => {
    const counts = { TOP: 0, JUG: 0, MID: 0, ADC: 0, SUP: 0 };
    if (!str) return counts;

    str.split(',').forEach(part => {
        const [role, val] = part.split(':');
        if (role && val) {
            // @ts-ignore
            const key = role.trim();
            // @ts-ignore
            if (counts.hasOwnProperty(key)) {
                // @ts-ignore
                counts[key] += parseInt(val, 10);
            }
        }
    });
    return counts;
};

export default function MatchDetailPage() {
    // 1. URL 파라미터 처리 (배열일 경우 첫 번째 값 사용)
    const params = useParams();
    const router = useRouter();
    const rawId = Array.isArray(params?.id) ? params.id[0] : params?.id;
    const currentMatchId = Number(rawId) || 0;

    // 2. 상태 관리
    const [selectedPattern, setSelectedPattern] = useState("질문(Q) ➡ 답변(I)");
    const [matchData, setMatchData] = useState<any>(null);
    const [loading, setLoading] = useState(true);
    const [timeRange, setTimeRange] = useState({ start: 0, end: Infinity }); // 단위: ms
    const [isUploadModalOpen, setIsUploadModalOpen] = useState(false);

    // 분석 지표 상태
    const [metrics, setMetrics] = useState<NetworkMetric[]>([]);
    const [exactDensity, setExactDensity] = useState<number>(0);

    // 3. 헬퍼 함수: 패턴 문자열 -> 소스/타겟 코드 변환
    const getPatternParams = (pattern: string) => {
        if (pattern === "전체 (ALL)") return { s: -1, t: -1 };
        if (pattern === "지시(D) ➡ 약속(C)") return { s: 2, t: 3 };
        if (pattern === "정보(I) ➡ 정보(I)") return { s: 0, t: 0 };
        if (pattern === "정보(I) ➡ 질문(Q)") return { s: 0, t: 1 };
        if (pattern === "정보(I) ➡ 지시(D)") return { s: 0, t: 2 };
        if (pattern === "약속(C) ➡ 정보(I)") return { s: 3, t: 0 };
        return { s: 1, t: 0 }; // 기본값: Q->I
    };

    // 4. API 1: 초기 매치 데이터 로드 (게임 정보, 로그, 이벤트)
    useEffect(() => {
        // [수정 후] 0번(초기 화면)이면 로딩 상태를 false로 바꾸고 데이터 비우기
        if (currentMatchId === 0) {
            setLoading(false);
            setMatchData(null);
            return;
        }

        const fetchGameData = async () => {
            setLoading(true);
            try {
                const response = await axios.get(`http://13.209.72.183/api/matches/${currentMatchId}`);
                setMatchData(response.data);

                // [추가] 초기 로딩 시 전체 기간으로 범위 설정 (Duration이 있으면)
                if (response.data.duration) {
                    setTimeRange({ start: 0, end: response.data.duration });
                }
            } catch (error) {
                console.error("데이터 로딩 실패:", error);
            } finally {
                setLoading(false);
            }
        };
        fetchGameData();
    }, [currentMatchId]);


    // 5. API 2: 패턴 변경 시 '전체 지표(Metrics)' 가져오기 (레이더 차트용)
    useEffect(() => {
        if (currentMatchId === 0) return;

        const fetchMetrics = async () => {
            const { s, t } = getPatternParams(selectedPattern);
            try {
                const res = await axios.get(`http://13.209.72.183/api/matches/${currentMatchId}/metrics`, {
                    params: { sourceDa: s, targetDa: t }
                });
                // 시간 인덱스(10초 단위)를 실제 초(sec)로 변환해서 저장
                const formatted = res.data.map((item: any) => ({
                    ...item,
                    realTimeSec: item.timeIndex * 10
                }));
                setMetrics(formatted);
            } catch (e) {
                console.error("Metrics Load Fail:", e);
            }
        };
        fetchMetrics();
    }, [selectedPattern, currentMatchId]);


    // 6. API 3: 타임라인(구간) 변경 시 '정밀 밀도(Density)' 요청
    useEffect(() => {
        // 데이터가 없거나 duration이 로드되지 않았으면 중단
        if (!matchData || !matchData.duration) return;

        const fetchRangeAnalysis = async () => {
            const { s, t } = getPatternParams(selectedPattern);

            // 타임라인 범위(ms)를 초(sec) 단위로 변환
            const startSec = Math.floor(timeRange.start / 1000);

            // end가 Infinity면 전체 시간 사용, 아니면 선택 범위 사용
            let endSec = 0;
            if (timeRange.end === Infinity || timeRange.end >= matchData.duration) {
                endSec = Math.floor(matchData.duration / 1000);
            } else {
                endSec = Math.floor(timeRange.end / 1000);
            }

            // ★ 방어 코드: 숫자가 아니거나 순서가 꼬이면 요청 스킵
            if (!Number.isFinite(startSec) || !Number.isFinite(endSec) || startSec >= endSec) {
                return;
            }

            try {
                const res = await axios.get(`http://13.209.72.183/api/matches/${currentMatchId}/analysis`, {
                    params: {
                        start: startSec,
                        end: endSec,
                        sourceDa: s,
                        targetDa: t
                    }
                });
                setExactDensity(res.data.density);
            } catch (e) {
                console.error("Analysis Request Fail:", e);
            }
        };

        // 디바운싱: 사용자가 드래그를 멈출 때까지 0.3초 대기
        const timer = setTimeout(fetchRangeAnalysis, 300);
        return () => clearTimeout(timer);

    }, [timeRange, selectedPattern, currentMatchId, matchData]);


    // 7. 로그 필터링 로직 (채팅창 표시용)
    // 7. 로그 필터링 로직 (수정본)
    const filteredLogs = useMemo(() => {
        // 데이터가 없으면 빈 배열
        if (!matchData?.voiceLogs) return [];

        let logs = matchData.voiceLogs;

        // (1) 시간 필터링
        if (timeRange.end !== Infinity) {
            logs = logs.filter((log: any) =>
                log.startTime >= timeRange.start && log.startTime <= timeRange.end
            );
        }

        // (2) 패턴 필터링
        const { s, t } = getPatternParams(selectedPattern);

        // "전체(ALL)" 패턴이면 필터링 없이 시간 맞는 거 다 보여줌
        if (s === -1 && t === -1) {
            return logs.sort((a: any, b: any) => a.startTime - b.startTime);
        }

        const resultLogs = new Set<any>();

        for (let i = 0; i < logs.length - 1; i++) {
            const current = logs[i];
            const next = logs[i + 1];

            // actCode나 actLabel을 안전하게 숫자로 변환
            const normalize = (val: any) => {
                if (typeof val === 'number') return val;
                if (val === 'I' || val === 'Info') return 0;
                if (val === 'Q' || val === 'Question') return 1;
                if (val === 'D' || val === 'Directive') return 2;
                if (val === 'C' || val === 'Commitment') return 3;
                return -99;
            };

            const curCode = normalize(current.actCode ?? current.actLabel);
            const nextCode = normalize(next.actCode ?? next.actLabel);

            // 패턴 매칭 (예: Q -> I)
            if (curCode === s && nextCode === t) {

                // ★ [수정] DTO에서 받은 position 필드를 사용
                // (만약 speakerName이 없으면 position으로 대체)
                const p1 = current.position || current.speakerName || "P1";
                const p2 = next.position || next.speakerName || "P2";

                // 자가 대화가 아닐 때만 추가 (서로 다른 사람)
                if (p1 !== p2) {
                    resultLogs.add(current);
                    resultLogs.add(next);
                }
            }
        }

        // 시간순 정렬해서 반환
        return Array.from(resultLogs).sort((a: any, b: any) => a.startTime - b.startTime);

    }, [matchData, timeRange, selectedPattern]);


    // 8. 하단 분석 지표 계산 (Radar 차트 합산)
    const dynamicAnalysis = useMemo(() => {
        const totalOut = { TOP: 0, JUG: 0, MID: 0, ADC: 0, SUP: 0 };
        const totalIn = { TOP: 0, JUG: 0, MID: 0, ADC: 0, SUP: 0 };

        // 현재 선택된 시간 범위 내의 metrics만 합산
        const filteredMetrics = metrics.filter(m =>
            (m.realTimeSec * 1000) >= timeRange.start &&
            (m.realTimeSec * 1000) <= timeRange.end
        );

        // 문자열 파싱 헬퍼 (예: "TOP:5,JUG:2")
        const parseCounts = (str: string) => {
            const res: any = { TOP: 0, JUG: 0, MID: 0, ADC: 0, SUP: 0 };
            if (!str) return res;
            str.split(',').forEach(part => {
                const [role, count] = part.split(':');
                if (role && count) res[role.trim()] = parseInt(count);
            });
            return res;
        };

        filteredMetrics.forEach(m => {
            const outs = parseCounts(m.positionDaCounts);
            const ins = parseCounts(m.positionReceiveCounts);

            (Object.keys(totalOut) as Array<keyof typeof totalOut>).forEach(r => {
                totalOut[r] += outs[r] || 0;
                totalIn[r] += ins[r] || 0;
            });
        });

        const roles = ['TOP', 'JUG', 'MID', 'ADC', 'SUP'];

        // 상태 텍스트 결정
        let stateText = 'Normal';
        if (exactDensity >= 0.3) stateText = '▲ High';
        else if (exactDensity > 0 && exactDensity < 0.1) stateText = '▼ Low';
        else if (exactDensity === 0) stateText = '- Silent';

        return {
            cod: roles.map(r => totalOut[r as keyof typeof totalOut]),
            cid: roles.map(r => totalIn[r as keyof typeof totalIn]),
            density: exactDensity, // API에서 받은 정밀 값 사용
            text: stateText
        };
    }, [metrics, timeRange, exactDensity]);

    // 9. 타임라인 props 계산 (밀리초 -> 초 변환)
    const totalDurationSec = matchData?.duration
        ? Math.floor(matchData.duration / 1000)
        : 0;


    // === 렌더링 ===
    if (loading) return (
        <div className="min-h-screen bg-slate-950 flex items-center justify-center text-white">
            <div className="text-center">
                <i className="fas fa-spinner fa-spin text-4xl text-blue-500 mb-4"></i>
                <p>Analyzing Match Data...</p>
            </div>
        </div>
    );

    return (
        // [수정 1] 최상위 div: p-6 제거하고 flex-col 적용 (탭 바를 꽉 채우기 위해)
        <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">

            {/* 모달은 위치 상관 없음 */}
            <UploadModal isOpen={isUploadModalOpen} onClose={() => setIsUploadModalOpen(false)} />

            {/* [수정 2] 탭 바 배치: 상단에 여백 없이 가로로 꽉 차게 들어감 */}
            <MatchTabs
                currentMatchId={params.id as string}
                onUploadClick={() => setIsUploadModalOpen(true)}
            />

            {/* [수정 3] 콘텐츠 래퍼 추가: 여기서 p-6(여백)과 max-w(최대 너비)를 줍니다 */}
            <div className="flex-1 p-6 w-full max-w-[1800px] mx-auto">

                {/* 헤더 */}
                <header className="flex justify-between items-center mb-6">
                    <div>
                        <h1 className="text-2xl font-bold text-blue-400">
                            <i className="fas fa-headset mr-2"></i>AI Coaching Assistant
                        </h1>
                        <p className="text-slate-400 text-sm mt-1">
                            Game Name: <span className="text-white font-bold text-lg ml-2">
                        {matchData?.matchCode || "Unknown"}
                    </span>
                        </p>
                    </div>

                    {/* ★ [삭제됨] 여기에 있던 "+ New Analysis" 버튼을 지웠습니다.
                   이유: 위쪽 MatchTabs에 이미 "+ 새 분석" 버튼을 만들었기 때문에 중복됩니다.
                */}
                </header>

                <main className="grid grid-cols-12 gap-6">

                    {/* 1. 네트워크 차트 */}
                    <div className="col-span-12 lg:col-span-8 bg-slate-900/50 border border-slate-800 rounded-xl p-4">
                        <NetworkChart
                            matchId={currentMatchId}
                            timeRange={timeRange}
                            events={matchData?.gameEvents || []}
                            selectedPattern={selectedPattern}
                            onPatternChange={setSelectedPattern}
                        />
                    </div>

                    {/* 2. 채팅 로그 */}
                    <div className="col-span-12 lg:col-span-4 row-span-3 h-full min-h-[600px] bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
                        <CommunicationLog logs={filteredLogs} />
                    </div>

                    {/* 3. 타임라인 */}
                    <div className="col-span-12 lg:col-span-8">
                        <div className="bg-slate-900/50 rounded-lg shadow-sm p-4 border border-slate-800">
                            <div className="mb-2 flex justify-end text-sm text-slate-400">
                                <span>Duration: {Math.floor(totalDurationSec / 60)}m {totalDurationSec % 60}s</span>
                            </div>

                            {totalDurationSec > 0 ? (
                                <InteractiveTimeline
                                    totalDuration={totalDurationSec}
                                    events={matchData?.gameEvents || []}
                                    selectedRange={timeRange}
                                    onRangeChange={setTimeRange}
                                />
                            ) : (
                                <div className="h-24 flex items-center justify-center text-slate-500 bg-slate-900 rounded">
                                    Timeline Data Unavailable
                                </div>
                            )}
                        </div>
                    </div>

                    {/* 4. 분석 지표 */}
                    <section className="col-span-12 lg:col-span-8 grid grid-cols-1 md:grid-cols-2 gap-6">
                        <DensityMetrics
                            density={dynamicAnalysis.density}
                            state={dynamicAnalysis.text}
                        />

                        <div className="bg-slate-900/50 border border-slate-800 rounded-xl p-6 flex flex-col items-center">
                            <div className="w-full mb-4">
                                <h3 className="font-semibold text-slate-200">Centralization Analysis</h3>
                                <p className="text-xs text-slate-400">Communication Balance by Role</p>
                            </div>
                            <div className="w-full h-[300px]">
                                <CentralizationRadar
                                    codData={dynamicAnalysis.cod}
                                    cidData={dynamicAnalysis.cid}
                                />
                            </div>
                        </div>
                    </section>
                </main>
            </div>
        </div>
    );
}

