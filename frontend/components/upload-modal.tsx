"use client";

import { useState } from 'react';
import axios from 'axios';
import { useRouter } from 'next/navigation';

export default function UploadModal({ isOpen, onClose }: { isOpen: boolean; onClose: () => void }) {
    const router = useRouter();

    // 1. 기존 상태 + 진영 선택 상태 추가
    const [selectedFile, setSelectedFile] = useState<File | null>(null);
    const [matchCode, setMatchCode] = useState('');
    const [selectedTeam, setSelectedTeam] = useState<'BLUE' | 'RED'>('BLUE'); // 추가된 부분
    const [uploading, setUploading] = useState(false);

    const handleUpload = async () => {
        if (!selectedFile || !matchCode) {
            alert("파일을 선택하고 매치 이름을 입력해주세요.");
            return;
        }

        setUploading(true);
        const formData = new FormData();

        // 2. 기존 데이터 전송 (유지)
        formData.append('zipFile', selectedFile);
        formData.append('matchCode', matchCode);

        // 3. 추가 데이터 전송 (진영 정보)
        formData.append('myTeam', selectedTeam);

        try {
            // 백엔드 엔드포인트 주소는 기존과 동일하게 유지하거나 필요시 수정하세요.
            const response = await axios.post(`${process.env.NEXT_PUBLIC_API_URL}/api/matches/import`, formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });

            alert("업로드 및 분석 성공!");
            onClose();
            router.push(`/matches/${response.data}`); // 상세 페이지 이동
        } catch (error) {
            console.error("Upload failed", error);
            alert("업로드 실패: 서버 설정을 확인해주세요.");
        } finally {
            setUploading(false);
        }
    };

    if (!isOpen) return null;

    return (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
            <div className="bg-slate-900 border border-slate-800 w-full max-w-md rounded-2xl p-6 shadow-xl">
                <h2 className="text-xl font-bold text-white mb-6">분석 데이터 업로드</h2>

                <div className="space-y-5">
                    {/* 매치 이름 (기존 유지) */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-2">Match Name</label>
                        <input
                            type="text"
                            value={matchCode}
                            onChange={(e) => setMatchCode(e.target.value)}
                            className="w-full bg-slate-950 border border-slate-800 rounded-lg px-4 py-2 text-white outline-none focus:ring-1 focus:ring-blue-500"
                            placeholder="매치 이름을 입력하세요"
                        />
                    </div>

                    {/* ★ 추가된 진영 선택 UI ★ */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-2">My Side (우리 팀 진영)</label>
                        <div className="flex gap-2">
                            <button
                                type="button"
                                onClick={() => setSelectedTeam('BLUE')}
                                className={`flex-1 py-2 rounded-lg border-2 font-bold transition-all ${
                                    selectedTeam === 'BLUE'
                                        ? 'border-blue-600 bg-blue-600/20 text-blue-400'
                                        : 'border-slate-800 bg-slate-950 text-slate-600'
                                }`}
                            >
                                BLUE
                            </button>
                            <button
                                type="button"
                                onClick={() => setSelectedTeam('RED')}
                                className={`flex-1 py-2 rounded-lg border-2 font-bold transition-all ${
                                    selectedTeam === 'RED'
                                        ? 'border-red-600 bg-red-600/20 text-red-400'
                                        : 'border-slate-800 bg-slate-950 text-slate-600'
                                }`}
                            >
                                RED
                            </button>
                        </div>
                    </div>

                    {/* 파일 선택 (기존 유지) */}
                    <div>
                        <label className="block text-sm font-medium text-slate-400 mb-2">ZIP File</label>
                        <input
                            type="file"
                            accept=".zip"
                            onChange={(e) => setSelectedFile(e.target.files?.[0] || null)}
                            className="w-full text-sm text-slate-500 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:bg-blue-600/10 file:text-blue-500 hover:file:bg-blue-600/20 cursor-pointer"
                        />
                    </div>
                </div>

                <div className="mt-8 flex gap-3">
                    <button onClick={onClose} className="flex-1 py-2 rounded-lg bg-slate-800 text-slate-300 hover:bg-slate-700">취소</button>
                    <button
                        onClick={handleUpload}
                        disabled={uploading}
                        className={`flex-1 py-2 rounded-lg font-bold text-white transition-all ${
                            uploading ? 'bg-slate-700 cursor-not-allowed' : 'bg-blue-600 hover:bg-blue-500'
                        }`}
                    >
                        {uploading ? '분석 중...' : '분석 시작'}
                    </button>
                </div>
            </div>
        </div>
    );
}