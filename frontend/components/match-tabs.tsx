"use client";

import { useRouter } from 'next/navigation';
import { Plus } from 'lucide-react';
import { useState, useEffect } from 'react';
import axios from 'axios';

interface MatchTabProps {
    currentMatchId: string;
    onUploadClick: () => void;
}

// DTO와 일치하는 타입 정의
interface MatchSummary {
    id: number;
    matchCode: string;
}

export default function MatchTabs({ currentMatchId, onUploadClick }: MatchTabProps) {
    const router = useRouter();
    const [matches, setMatches] = useState<MatchSummary[]>([]);

    // ★ 컴포넌트 로딩 시 DB에서 목록 가져오기
    useEffect(() => {
        const fetchMatches = async () => {
            try {
                const res = await axios.get(`${process.env.NEXT_PUBLIC_API_URL}/api/matches/list`);                setMatches(res.data);
            } catch (err) {
                console.error("매치 목록 로딩 실패:", err);
            }
        };

        fetchMatches();
    }, []);

    return (
        <div className="w-full bg-slate-950 border-b border-slate-800 flex items-center px-2 h-12 overflow-x-auto scrollbar-hide">

            {/* 새 분석 버튼 */}
            <button
                onClick={onUploadClick}
                className="flex items-center gap-2 px-3 py-1.5 mr-2 text-xs font-bold text-slate-300 bg-slate-800 hover:bg-blue-600 hover:text-white rounded-md transition-all shrink-0"
            >
                <Plus size={14} />
                <span>새 분석</span>
            </button>

            {/* 매치 탭 리스트 (DB 연동) */}
            <div className="flex items-center gap-1 h-full">
                {matches.map((match) => {
                    // 현재 탭인지 확인 (문자열 변환 비교)
                    const isActive = String(match.id) === currentMatchId;

                    return (
                        <button
                            key={match.id}
                            onClick={() => router.push(`/matches/${match.id}`)}
                            className={`
                relative h-full px-4 flex items-center text-sm font-medium transition-all min-w-[100px] justify-center shrink-0
                ${isActive
                                ? 'text-blue-400 border-b-2 border-blue-500 bg-slate-900/50'
                                : 'text-slate-500 hover:text-slate-300 hover:bg-slate-900'}
              `}
                        >
              <span className="truncate max-w-[150px]">
                  {match.matchCode || `Game ${match.id}`}
              </span>
                        </button>
                    );
                })}
            </div>
        </div>
    );
}