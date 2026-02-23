"use client";

import { useRouter } from 'next/navigation';
import { Plus, Trash2, X } from 'lucide-react'; // 아이콘 추가
import { useState, useEffect } from 'react';
import axios from 'axios';

interface MatchTabProps {
    currentMatchId: string;
    onUploadClick: () => void;
}

interface MatchSummary {
    id: number;
    matchCode: string;
}

export default function MatchTabs({ currentMatchId, onUploadClick }: MatchTabProps) {
    const router = useRouter();
    const [matches, setMatches] = useState<MatchSummary[]>([]);

    // --- 추가된 상태값 ---
    const [isModalOpen, setIsModalOpen] = useState(false);
    const [deleteTargetId, setDeleteTargetId] = useState<number | null>(null);
    const [selectedIds, setSelectedIds] = useState<number[]>([]);

    useEffect(() => {
        const fetchMatches = async () => {
            try {
                const res = await axios.get(`${process.env.NEXT_PUBLIC_API_URL}/api/matches/list`);
                setMatches(res.data);
            } catch (err) {
                console.error("매치 목록 로딩 실패:", err);
            }
        };
        fetchMatches();
    }, []);

    // --- 삭제 관련 함수 ---
    const openDeleteModal = (e: React.MouseEvent, id: number) => {
        e.stopPropagation(); // 탭 이동 방지
        setDeleteTargetId(id);
        setIsModalOpen(true);
    };

    const confirmDelete = async () => {
        if (!deleteTargetId) return;
        try {
            await axios.delete(`${process.env.NEXT_PUBLIC_API_URL}/api/matches/${deleteTargetId}`);
            setMatches(matches.filter(m => m.id !== deleteTargetId));
            setIsModalOpen(false);
            setDeleteTargetId(null);

            // 만약 현재 보고 있는 페이지를 삭제했다면 목록 첫 페이지나 메인으로 이동
            if (String(deleteTargetId) === currentMatchId) {
                router.push('/');
            }
        } catch (err) {
            console.error("삭제 실패:", err);
            alert("삭제 중 오류가 발생했습니다.");
        }
    };

    const toggleCheckbox = (e: React.ChangeEvent<HTMLInputElement>, id: number) => {
        e.stopPropagation(); // 탭 클릭 방지
        if (e.target.checked) {
            setSelectedIds([...selectedIds, id]);
        } else {
            setSelectedIds(selectedIds.filter(selectedId => selectedId !== id));
        }
    };

    return (
        <div className="w-full bg-slate-950 border-b border-slate-800 flex items-center px-2 h-12 overflow-x-auto scrollbar-hide relative">

            {/* 새 분석 버튼 */}
            <button
                onClick={onUploadClick}
                className="flex items-center gap-2 px-3 py-1.5 mr-2 text-xs font-bold text-slate-300 bg-slate-800 hover:bg-blue-600 hover:text-white rounded-md transition-all shrink-0"
            >
                <Plus size={14} />
                <span>새 분석</span>
            </button>

            {/* 매치 탭 리스트 */}
            <div className="flex items-center gap-1 h-full">
                {matches.map((match) => {
                    const isActive = String(match.id) === currentMatchId;

                    return (
                        <div
                            key={match.id}
                            className={`group relative h-full flex items-center transition-all min-w-[140px] border-b-2 shrink-0
                                ${isActive ? 'bg-slate-900/50 border-blue-500' : 'border-transparent hover:bg-slate-900'}
                            `}
                        >

                            {/* 탭 텍스트 (클릭 시 이동) */}
                            <button
                                onClick={() => router.push(`/matches/${match.id}`)}
                                className={`flex-1 h-full px-2 flex items-center text-sm font-medium justify-start truncate
                                    ${isActive ? 'text-blue-400' : 'text-slate-500 hover:text-slate-300'}
                                `}
                            >
                                <span className="truncate max-w-[100px]">
                                    {match.matchCode || `Game ${match.id}`}
                                </span>
                            </button>

                            {/* 2. 삭제 버튼 (오른쪽 끝, 호버 시 강조) */}
                            <button
                                onClick={(e) => openDeleteModal(e, match.id)}
                                className="px-2 text-slate-600 hover:text-red-500 transition-colors z-10"
                            >
                                <Trash2 size={14} />
                            </button>
                        </div>
                    );
                })}
            </div>

            {/* 3. 삭제 확인 모달 */}
            {isModalOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center bg-black/70 backdrop-blur-sm">
                    <div className="bg-slate-900 border border-slate-800 p-6 rounded-xl shadow-2xl w-[320px] animate-in fade-in zoom-in duration-200">
                        <div className="flex justify-center mb-4 text-red-500">
                            <Trash2 size={40} />
                        </div>
                        <h3 className="text-lg font-bold text-white text-center mb-2">삭제하시겠습니까?</h3>
                        <p className="text-slate-400 text-sm text-center mb-6">이 작업은 되돌릴 수 없으며 모든 데이터가 삭제됩니다.</p>

                        <div className="flex gap-3">
                            <button
                                onClick={() => setIsModalOpen(false)}
                                className="flex-1 py-2 text-sm font-medium text-slate-300 bg-slate-800 hover:bg-slate-700 rounded-lg transition-colors"
                            >
                                취소
                            </button>
                            <button
                                onClick={confirmDelete}
                                className="flex-1 py-2 text-sm font-medium text-white bg-red-600 hover:bg-red-700 rounded-lg transition-colors"
                            >
                                삭제
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}