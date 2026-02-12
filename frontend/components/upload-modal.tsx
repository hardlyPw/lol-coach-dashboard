"use client";

import { useState } from 'react';
import axios from 'axios';
import { useRouter } from 'next/navigation';

interface UploadModalProps {
    isOpen: boolean;
    onClose: () => void;
}

export default function UploadModal({ isOpen, onClose }: UploadModalProps) {
    const [file, setFile] = useState<File | null>(null);
    const [gameName, setGameName] = useState(""); // ★ [추가] 게임 이름 상태
    const [uploading, setUploading] = useState(false);
    const router = useRouter();

    if (!isOpen) return null;

    const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        if (e.target.files && e.target.files[0]) {
            setFile(e.target.files[0]);
        }
    };

    const handleUpload = async () => {
        if (!file) return;

        // 이름이 없으면 파일명이나 기본값 사용
        const finalName = gameName.trim() || file.name.replace(".zip", "");

        const formData = new FormData();
        formData.append("file", file);
        formData.append("matchCode", finalName); // ★ [추가] 백엔드로 이름 전송

        setUploading(true);
        try {
            // 주소 확인 (MatchController의 주소와 일치해야 함)
            const response = await axios.post("http://3.34.82.181:8080/api/matches", formData, {
                headers: { "Content-Type": "multipart/form-data" },
            });

            alert("업로드 성공!");
            onClose();
            // 업로드 된 페이지로 이동
            router.push(`/matches/${response.data}`);
        } catch (error) {
            console.error("Upload failed", error);
            alert("업로드 실패");
        } finally {
            setUploading(false);
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-slate-900 p-6 rounded-lg border border-slate-700 w-96 text-white">
                <h2 className="text-xl font-bold mb-4">새 게임 분석 (Upload)</h2>

                {/* ★ [추가] 게임 이름 입력 인풋 */}
                <div className="mb-4">
                    <label className="block text-sm text-slate-400 mb-1">Game Name</label>
                    <input
                        type="text"
                        className="w-full bg-slate-800 border border-slate-600 rounded p-2 text-white"
                        placeholder="예: T1 vs GEN 3세트"
                        value={gameName}
                        onChange={(e) => setGameName(e.target.value)}
                    />
                </div>

                <div className="mb-6">
                    <label className="block text-sm text-slate-400 mb-1">Data File (.zip)</label>
                    <input
                        type="file"
                        accept=".zip"
                        onChange={handleFileChange}
                        className="w-full text-sm text-slate-300 file:mr-4 file:py-2 file:px-4 file:rounded-full file:border-0 file:text-sm file:font-semibold file:bg-blue-600 file:text-white hover:file:bg-blue-700"
                    />
                </div>

                <div className="flex justify-end gap-2">
                    <button onClick={onClose} className="px-4 py-2 text-slate-300 hover:text-white">취소</button>
                    <button
                        onClick={handleUpload}
                        disabled={uploading || !file}
                        className="px-4 py-2 bg-blue-600 rounded hover:bg-blue-500 disabled:opacity-50"
                    >
                        {uploading ? "업로드 중..." : "분석 시작"}
                    </button>
                </div>
            </div>
        </div>
    );
}