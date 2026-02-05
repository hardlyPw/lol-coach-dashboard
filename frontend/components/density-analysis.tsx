export function DensityAnalysis() {
  return (
    <div className="rounded-lg border border-border bg-card p-6">
      <h2 className="mb-4 text-lg font-semibold text-card-foreground">구간 분석 요약</h2>

      <div className="grid gap-4 md:grid-cols-2">
        {/* Decision Making Density */}
        <div>
          <p className="mb-2 text-sm text-muted-foreground">Decision Making Density</p>
          <div className="mb-2 flex items-baseline gap-2">
            <span className="text-3xl font-bold text-yellow-500">0.32</span>
            <span className="flex items-center gap-1 text-sm text-red-400">
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
              </svg>
              Low
            </span>
          </div>
          <div className="h-2 w-full rounded-full bg-secondary">
            <div className="h-2 w-[32%] rounded-full bg-yellow-500" />
          </div>
        </div>

        {/* Q&A Density */}
        <div>
          <p className="mb-2 text-sm text-muted-foreground">Q&A Density</p>
          <div className="mb-2 flex items-baseline gap-2">
            <span className="text-3xl font-bold text-blue-400">0.44</span>
            <span className="flex items-center gap-1 text-sm text-green-400">
              <svg className="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 10l7-7m0 0l7 7m-7-7v18" />
              </svg>
              Good
            </span>
          </div>
          <div className="h-2 w-full rounded-full bg-secondary">
            <div className="h-2 w-[44%] rounded-full bg-blue-400" />
          </div>
        </div>
      </div>
    </div>
  )
}
