"use client"

import { Radar, RadarChart, PolarGrid, PolarAngleAxis, ResponsiveContainer } from "recharts"

export function PositionRadar() {
  const data = [
    { position: "Top", current: 85, average: 70 },
    { position: "Jungle", current: 75, average: 65 },
    { position: "Mid", current: 90, average: 75 },
    { position: "AD Carry", current: 80, average: 68 },
    { position: "Support", current: 88, average: 72 },
  ]

  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <h2 className="mb-6 text-lg font-semibold text-card-foreground">Density</h2>

      <ResponsiveContainer width="100%" height={300}>
        <RadarChart data={data}>
          <PolarGrid stroke="#374151" />
          <PolarAngleAxis dataKey="position" tick={{ fill: "#9ca3af", fontSize: 12 }} />
          <Radar name="League Avg" dataKey="average" stroke="#6b7280" fill="#6b7280" fillOpacity={0.3} />
          <Radar name="Current Team" dataKey="current" stroke="#3b82f6" fill="#3b82f6" fillOpacity={0.5} />
        </RadarChart>
      </ResponsiveContainer>

      <div className="mt-4 flex items-center justify-center gap-6 text-sm">
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded-sm bg-blue-500" />
          <span className="text-muted-foreground">Current Team</span>
        </div>
        <div className="flex items-center gap-2">
          <div className="h-3 w-3 rounded-sm bg-gray-500" />
          <span className="text-muted-foreground">League Avg</span>
        </div>
      </div>
    </div>
  )
}
