import { useState } from 'react'

export type FilterOptions = {
  status: string
  priority: string
  keyword: string
}

type TaskFiltersProps = {
  onFilterChange: (filters: FilterOptions) => void
  totalCount: number
  filteredCount: number
}

export function TaskFilters({ onFilterChange, totalCount, filteredCount }: TaskFiltersProps) {
  const [filters, setFilters] = useState<FilterOptions>({
    status: 'ALL',
    priority: 'ALL',
    keyword: ''
  })

  const handleStatusChange = (value: string) => {
    const newFilters = { ...filters, status: value }
    setFilters(newFilters)
    onFilterChange(newFilters)
  }

  const handlePriorityChange = (value: string) => {
    const newFilters = { ...filters, priority: value }
    setFilters(newFilters)
    onFilterChange(newFilters)
  }

  const handleKeywordChange = (value: string) => {
    const newFilters = { ...filters, keyword: value }
    setFilters(newFilters)
    onFilterChange(newFilters)
  }

  const handleReset = () => {
    const resetFilters = { status: 'ALL', priority: 'ALL', keyword: '' }
    setFilters(resetFilters)
    onFilterChange(resetFilters)
  }

  const hasActiveFilters = filters.status !== 'ALL' || filters.priority !== 'ALL' || filters.keyword !== ''

  return (
    <div className="task-filters">
      <div className="filters-row">
        <div className="filter-group">
          <label htmlFor="status-filter">状态</label>
          <select
            id="status-filter"
            value={filters.status}
            onChange={(e) => handleStatusChange(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">全部状态</option>
            <option value="TODO">待处理</option>
            <option value="IN_PROGRESS">进行中</option>
            <option value="DONE">已完成</option>
          </select>
        </div>

        <div className="filter-group">
          <label htmlFor="priority-filter">优先级</label>
          <select
            id="priority-filter"
            value={filters.priority}
            onChange={(e) => handlePriorityChange(e.target.value)}
            className="filter-select"
          >
            <option value="ALL">全部优先级</option>
            <option value="HIGH">高</option>
            <option value="MEDIUM">中</option>
            <option value="LOW">低</option>
          </select>
        </div>

        <div className="filter-group filter-group--search">
          <label htmlFor="keyword-search">搜索</label>
          <input
            id="keyword-search"
            type="text"
            placeholder="按标题或描述搜索..."
            value={filters.keyword}
            onChange={(e) => handleKeywordChange(e.target.value)}
            className="filter-search"
          />
        </div>

        {hasActiveFilters && (
          <button type="button" onClick={handleReset} className="filter-reset">
            清除筛选
          </button>
        )}
      </div>

      <div className="filter-stats">
        <span>
          显示 <strong>{filteredCount}</strong> / <strong>{totalCount}</strong> 个任务
        </span>
        {hasActiveFilters && filteredCount === 0 && (
          <span className="filter-stats--warning">没有匹配的任务，试试其他筛选条件</span>
        )}
      </div>
    </div>
  )
}