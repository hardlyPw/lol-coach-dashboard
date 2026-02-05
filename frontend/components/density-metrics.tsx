'use client'

interface DensityMetricsProps {
  density: number
  state: string
}

export function DensityMetrics({ density, state }: DensityMetricsProps) {
  const isHigh = density >= 0.4
  const densityColor = isHigh ? 'text-blue-400' : 'text-yellow-400'
  const barColor = isHigh
    ? 'bg-blue-400 shadow-[0_0_10px_rgba(96,165,250,0.7)]'
    : 'bg-yellow-400 shadow-[0_0_10px_rgba(250,204,21,0.7)]'

  return (
    <div className="glass-panel rounded-xl p-6">
      <h3 className="text-lg font-bold mb-4 text-slate-200">Density</h3>
      <div className="bg-slate-800 p-6 rounded-lg border border-slate-700 flex flex-col justify-center items-center">
        <p className="text-sm text-slate-400 mb-2">Communication Density</p>
        <div className="flex items-baseline gap-2">
          <span className={`text-5xl font-bold ${densityColor}`}>{density.toFixed(2)}</span>
          <span className="text-sm text-green-400">{state}</span>
        </div>
        <div className="w-full bg-slate-700 h-2 mt-4 rounded-full overflow-hidden">
          <div
            className={`${barColor} h-full transition-all duration-500`}
            style={{ width: `${density * 100}%` }}
          ></div>
        </div>
      </div>
    </div>
  )
}
