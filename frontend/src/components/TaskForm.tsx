import { useEffect, useState } from 'react'
import type { TaskFormValues } from '../types/task'
import {
  PRIORITY_OPTIONS,
  STATUS_OPTIONS,
  emptyTaskFormValues,
} from '../types/task'

type TaskFormProps = {
  mode: 'create' | 'edit'
  initialValues?: TaskFormValues
  error?: string
  isSubmitting: boolean
  onCancelCreate: () => void
  onDelete?: () => void
  onSubmit: (values: TaskFormValues) => Promise<void> | void
}

export function TaskForm({
  mode,
  initialValues = emptyTaskFormValues,
  error,
  isSubmitting,
  onCancelCreate,
  onDelete,
  onSubmit,
}: TaskFormProps) {
  const [formValues, setFormValues] = useState(initialValues)

  useEffect(() => {
    setFormValues(initialValues)
  }, [initialValues])
  useEffect(() => {
    if (mode === 'create') {
      setFormValues(emptyTaskFormValues)
    }
  }, [mode])
  
  const isCreateMode = mode === 'create'

  const handleChange = (
    event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement | HTMLSelectElement>,
  ) => {
    const { name, value } = event.target
    setFormValues((current) => ({
      ...current,
      [name]: value,
    }))
  }

  const handleSubmit = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    await onSubmit(formValues)
  }

  const handleReset = () => {
    if (isCreateMode) {
      setFormValues(emptyTaskFormValues)
      onCancelCreate()
      return
    }

    setFormValues(initialValues)
  }

  return (
    <form className="task-form" onSubmit={handleSubmit}>
      <div className="field">
        <label htmlFor="title">标题</label>
        <input
          id="title"
          name="title"
          maxLength={120}
          placeholder="例如：完成用户登录接口联调"
          required
          value={formValues.title}
          onChange={handleChange}
        />
      </div>

      <div className="field">
        <label htmlFor="description">描述</label>
        <textarea
          id="description"
          name="description"
          maxLength={1000}
          placeholder="补充任务背景、验收标准、备注信息"
          value={formValues.description}
          onChange={handleChange}
        />
      </div>

      <div className="field-grid">
        <div className="field">
          <label htmlFor="status">状态</label>
          <select
            id="status"
            name="status"
            value={formValues.status}
            onChange={handleChange}
          >
            {STATUS_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>

        <div className="field">
          <label htmlFor="priority">优先级</label>
          <select
            id="priority"
            name="priority"
            value={formValues.priority}
            onChange={handleChange}
          >
            {PRIORITY_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
        </div>
      </div>

      <div className="field">
        <label htmlFor="dueAt">截止时间</label>
        <input
          id="dueAt"
          name="dueAt"
          type="datetime-local"
          value={formValues.dueAt}
          onChange={handleChange}
        />
      </div>

      {error ? <div className="message message--error">{error}</div> : null}

      <div className="form-actions">
        <button className="button-primary" type="submit" disabled={isSubmitting}>
          {isSubmitting ? '提交中...' : isCreateMode ? '创建任务' : '保存修改'}
        </button>
        <button
          className="button-ghost"
          type="button"
          disabled={isSubmitting}
          onClick={handleReset}
        >
          {isCreateMode ? '清空草稿' : '恢复当前内容'}
        </button>
        {!isCreateMode && onDelete ? (
          <button
            className="button-danger"
            type="button"
            disabled={isSubmitting}
            onClick={onDelete}
          >
            删除任务
          </button>
        ) : null}
      </div>
    </form>
  )
}
