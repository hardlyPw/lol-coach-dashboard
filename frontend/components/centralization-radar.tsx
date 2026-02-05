'use client'

import {
  Chart as ChartJS,
  RadialLinearScale,
  PointElement,
  LineElement,
  Filler,
  Tooltip,
  Legend,
} from 'chart.js'
import { Radar } from 'react-chartjs-2'

// Chart.js 모듈 등록
ChartJS.register(
    RadialLinearScale,
    PointElement,
    LineElement,
    Filler,
    Tooltip,
    Legend
)

interface CentralizationRadarProps {
  codData: number[] // Out-Degree (말 건 횟수)
  cidData: number[] // In-Degree (말 들은 횟수)
}

export function CentralizationRadar({ codData, cidData }: CentralizationRadarProps) {

  // 데이터가 아예 없을 때를 대비한 방어 로직 (0으로 채움)
  const safeCod = codData?.length === 5 ? codData : [0, 0, 0, 0, 0];
  const safeCid = cidData?.length === 5 ? cidData : [0, 0, 0, 0, 0];

  const data = {
    labels: ['TOP', 'JUG', 'MID', 'ADC', 'SUP'],
    datasets: [
      {
        label: 'Out (말 건 횟수)', // 🔴 빨간색
        data: safeCod,
        backgroundColor: 'rgba(239, 68, 68, 0.2)', // Red-500 투명도 20%
        borderColor: '#ef4444',                    // Red-500
        borderWidth: 2,
        pointBackgroundColor: '#ef4444',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: '#ef4444',
      },
      {
        label: 'In (말 들은 횟수)', // 🔵 파란색
        data: safeCid,
        backgroundColor: 'rgba(59, 130, 246, 0.2)', // Blue-500 투명도 20%
        borderColor: '#3b82f6',                     // Blue-500
        borderWidth: 2,
        pointBackgroundColor: '#3b82f6',
        pointBorderColor: '#fff',
        pointHoverBackgroundColor: '#fff',
        pointHoverBorderColor: '#3b82f6',
      },
    ],
  }

  const options = {
    responsive: true,
    maintainAspectRatio: false,
    scales: {
      r: {
        angleLines: {
          color: 'rgba(255, 255, 255, 0.1)',
        },
        grid: {
          color: 'rgba(255, 255, 255, 0.1)',
        },
        pointLabels: {
          color: '#94a3b8', // slate-400
          font: {
            size: 12,
            weight: 'bold' as const, // 'bold' string 문제 해결을 위해 as const 사용
          },
        },
        ticks: {
          display: false, // 눈금 숫자 숨김 (깔끔하게)
          backdropColor: 'transparent',
        },
        // 데이터 최대값에 따라 그래프 크기 자동 조절 (너무 작게 나오는 것 방지)
        suggestedMin: 0,
        suggestedMax: 5,
      },
    },
    plugins: {
      legend: {
        display: false, // 기본 범례 숨기고 커스텀 범례 사용
      },
      tooltip: {
        backgroundColor: '#1e293b',
        titleColor: '#fff',
        bodyColor: '#cbd5e1',
        borderColor: '#334155',
        borderWidth: 1,
      }
    },
  }

  return (
      <div className="w-full h-full flex flex-col items-center justify-center relative">

        {/* ★ [추가된 부분] 커스텀 범례 (Legend) */}
        <div className="flex gap-6 mb-4">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-red-500 shadow-[0_0_8px_rgba(239,68,68,0.6)]"></span>
            <span className="text-xs text-slate-300 font-medium">Out (발화/주도)</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full bg-blue-500 shadow-[0_0_8px_rgba(59,130,246,0.6)]"></span>
            <span className="text-xs text-slate-300 font-medium">In (수신/호응)</span>
          </div>
        </div>

        {/* 차트 영역 */}
        <div className="w-full h-[300px]">
          <Radar data={data} options={options} />
        </div>
      </div>
  )
}