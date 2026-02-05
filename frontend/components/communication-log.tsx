'use client'

import React, { useRef, useEffect } from 'react';

interface CommunicationLogProps {
  logs?: any[]
}

export function CommunicationLog({ logs = [] }: CommunicationLogProps) {

  const scrollRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs]);

  const formatTime = (ms: number) => {
    if (!ms) return "00:00";
    const totalSeconds = Math.floor(ms / 1000);
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${minutes}:${seconds.toString().padStart(2, '0')}`;
  };

  const formatPosition = (pos: string) => {
    if (!pos) return "UNK"; // 데이터 없을 때 처리
    const p = pos.toUpperCase();
    if (p.includes("TOP")) return "TOP";
    if (p.includes("JUNGLE") || p.includes("JUG")) return "JUG";
    if (p.includes("MID")) return "MID";
    if (p.includes("AD") || p.includes("BOT")) return "ADC";
    if (p.includes("SUP")) return "SUP";
    return p.substring(0, 3);
  }

  const getStyleByLabel = (label: string) => {
    switch (label) {
      case 'Q': return { borderColor: 'border-blue-500', icon: 'fa-question', iconColor: 'text-blue-400', badgeStyle: 'text-blue-300 border-blue-500/30 bg-blue-500/10' };
      case 'D': return { borderColor: 'border-red-500', icon: 'fa-gavel', iconColor: 'text-red-400', badgeStyle: 'text-red-300 border-red-500/30 bg-red-500/10' };
      case 'I': return { borderColor: 'border-green-500', icon: 'fa-info', iconColor: 'text-green-400', badgeStyle: 'text-green-300 border-green-500/30 bg-green-500/10' };
      default: return { borderColor: 'border-slate-500', icon: 'fa-comment', iconColor: 'text-slate-400', badgeStyle: 'text-slate-300 border-slate-500/30 bg-slate-500/10' };
    }
  };

  return (
      <section className="col-span-12 lg:col-span-7 glass-panel rounded-xl p-6 h-full flex flex-col">
        <h3 className="text-lg font-bold mb-4 flex items-center flex-shrink-0">
          <i className="fas fa-comments text-blue-400 mr-2"></i> 음성/채팅 분석 로그
          <span className="ml-2 text-xs bg-slate-800 px-2 py-1 rounded text-slate-400">
          Total: {logs.length}
        </span>
        </h3>

        {/* ★ [수정] flex-grow와 함께 'h-0'을 넣어야 박스 안에서만 스크롤됩니다! */}
        <div ref={scrollRef} className="space-y-3 overflow-y-auto pr-2 scrollbar-hide flex-grow h-0">
          {logs.length === 0 && (
              <div className="text-center text-slate-500 py-10">
                데이터를 불러오는 중이거나 해당 구간에 로그가 없습니다.
              </div>
          )}

          {logs.map((log, index) => {
            const style = getStyleByLabel(log.actLabel);

            return (
                <div
                    key={index}
                    className={`flex gap-3 p-3 bg-slate-800/40 rounded-lg border-l-4 ${style.borderColor} hover:bg-slate-800 transition group`}
                >
                  <div className="flex-shrink-0 text-center w-10">
                    <div className="w-8 h-8 mx-auto rounded bg-slate-700/50 flex items-center justify-center mb-1 group-hover:bg-slate-700 transition">
                      <i className={`fas ${style.icon} ${style.iconColor} text-xs`}></i>
                    </div>
                    <span className="text-[10px] text-slate-500 font-mono">
                      {formatTime(log.startTime)}
                    </span>
                  </div>

                  <div className="flex-grow">
                    <div className="flex justify-between items-start mb-1">
                      <div className="flex items-center gap-2">
                        {/* ★ [수정] log.player가 있는지 확인하고 출력 */}
                        {/* ✅ [수정 후] 백엔드 DTO 변수명(speakerName, position)과 똑같이 맞춰야 함! */}
                        <span className="font-bold text-slate-200 text-sm">
                          {log.speakerName || "Unknown"}
                        </span>
                        <span className="text-[10px] text-slate-500 uppercase font-mono bg-slate-900 px-1 rounded">
                          {formatPosition(log.position)}
                        </span>
                      </div>

                      <span className={`px-2 py-0.5 rounded text-[10px] font-bold border ${style.badgeStyle}`}>
                        {log.actLabel}
                      </span>
                    </div>
                    <p className="text-slate-300 text-sm leading-relaxed">
                      {log.textKor}
                    </p>
                  </div>
                </div>
            );
          })}
        </div>
      </section>
  )
}