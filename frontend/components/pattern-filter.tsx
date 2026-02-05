'use client'

interface PatternFilterProps {
  selected: string
  onSelect: (pattern: string) => void
}

export function PatternFilter({ selected, onSelect }: PatternFilterProps) {
  const patterns = [
    { value: 'ALL', label: 'All Patterns' },
    { value: 'I-I', label: 'I(Inform) → I(Inform)' },
    { value: 'Q-I', label: 'Q(Question) → I(Inform)' },
    { value: 'D-C', label: 'D(Directive) → C (Commissive)' },
    { value: 'I-D', label: 'I(Inform) → D(Directive)' },
    { value: 'I-Q', label: 'I(Inform) → Q(Question)' },
    { value: 'C-I', label: 'C(Commissive) → I(Inform)' },
  ]

  return (
      // 불필요한 배경(glass-panel)과 텍스트 설명 제거
      // 오직 드롭다운 박스만 남김
      <div className="relative w-56"> {/* 너비도 살짝 조정 (w-64 -> w-56) */}
        <div className="absolute inset-y-0 left-0 flex items-center pl-3 pointer-events-none">
          <i className="fas fa-filter text-slate-400 text-xs"></i>
        </div>

        <select
            value={selected}
            onChange={(e) => onSelect(e.target.value)}
            className="bg-slate-800 border border-slate-600 text-slate-200 text-sm rounded-lg focus:ring-blue-500 focus:border-blue-500 block w-full pl-9 p-2 appearance-none cursor-pointer hover:bg-slate-700 transition"
        >
          {patterns.map((pattern) => (
              <option key={pattern.value} value={pattern.value}>
                {pattern.label}
              </option>
          ))}
        </select>

        <div className="absolute inset-y-0 right-0 flex items-center pr-3 pointer-events-none text-slate-400">
          <i className="fas fa-chevron-down text-xs"></i>
        </div>
      </div>
  )
}