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
    keyword: '',
  })

  const updateFilters = (nextFilters: FilterOptions) => {
    setFilters(nextFilters)
    onFilterChange(nextFilters)
  }

  const hasActiveFilters = filters.status !== 'ALL' || filters.priority !== 'ALL' || filters.keyword !== ''

  return (
    <section className="task-filters">
      <div className="filters-row">
        <div className="filter-group">
          <label htmlFor="status-filter">状态</label>
          <select
            id="status-filter"
            value={filters.status}
            onChange={(event) => updateFilters({ ...filters, status: event.target.value })}
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
            onChange={(event) => updateFilters({ ...filters, priority: event.target.value })}
          >
            <option value="ALL">全部优先级</option>
            <option value="HIGH">高</option>
            <option value="MEDIUM">中</option>
            <option value="LOW">低</option>
          </select>
        </div>

        <div className="filter-group filter-group--search">
          <label htmlFor="keyword-filter">关键词</label>
          <input
            id="keyword-filter"
            value={filters.keyword}
            placeholder="按标题、描述、团队名搜索"
            onChange={(event) => updateFilters({ ...filters, keyword: event.target.value })}
          />
        </div>

        {hasActiveFilters ? (
          <button
            className="button-ghost"
            type="button"
            onClick={() => updateFilters({ status: 'ALL', priority: 'ALL', keyword: '' })}
          >
            清除筛选
          </button>
        ) : null}
      </div>

      <div className="filter-stats">
        <span>
          当前展示 <strong>{filteredCount}</strong> / <strong>{totalCount}</strong> 条任务
        </span>
        {hasActiveFilters && filteredCount === 0 ? (
          <span className="filter-stats--warning">没有匹配的任务，试试放宽筛选条件</span>
        ) : null}
      </div>
    </section>
  )
}
