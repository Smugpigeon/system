import { useEffect, useState } from 'react'
import type { TaskFormValues } from '../types/task'
import {
  PRIORITY_OPTIONS,
  STATUS_OPTIONS,
  emptyTaskFormValues,
} from '../types/task'

type AssigneeOption = {
  value: string
  label: string
}

type TaskFormProps = {
  mode: 'create' | 'edit'
  initialValues?: TaskFormValues
  error?: string
  isSubmitting: boolean
  onCancelCreate: () => void
  onDelete?: () => void
  onSubmit: (values: TaskFormValues) => Promise<void> | void
  allowDetailEditing?: boolean
  allowStatusEditing?: boolean
  allowDelete?: boolean
  showAssignee?: boolean
  assigneeOptions?: AssigneeOption[]
  readOnlyHint?: string
  submitLabel?: string
}

export function TaskForm({
  mode,
  initialValues = emptyTaskFormValues,
  error,
  isSubmitting,
  onCancelCreate,
  onDelete,
  onSubmit,
  allowDetailEditing = true,
  allowStatusEditing = true,
  allowDelete = Boolean(onDelete),
  showAssignee = false,
  assigneeOptions = [],
  readOnlyHint = '',
  submitLabel,
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
  const canSubmit = isCreateMode || allowDetailEditing || allowStatusEditing

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
    if (!canSubmit) {
      return
    }
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
          placeholder="例如：完成团队空间联调"
          required
          disabled={!isCreateMode && !allowDetailEditing}
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
          disabled={!isCreateMode && !allowDetailEditing}
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
            disabled={!isCreateMode && !allowStatusEditing}
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
            disabled={!isCreateMode && !allowDetailEditing}
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

      <div className="field-grid">
        <div className="field">
          <label htmlFor="dueAt">截止时间</label>
          <input
            id="dueAt"
            name="dueAt"
            type="datetime-local"
            disabled={!isCreateMode && !allowDetailEditing}
            value={formValues.dueAt}
            onChange={handleChange}
          />
        </div>

        {showAssignee ? (
          <div className="field">
            <label htmlFor="assigneeId">负责人</label>
            <select
              id="assigneeId"
              name="assigneeId"
              required
              disabled={!isCreateMode && !allowDetailEditing}
              value={formValues.assigneeId}
              onChange={handleChange}
            >
              <option value="">请选择负责人</option>
              {assigneeOptions.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>
        ) : null}
      </div>

      {readOnlyHint ? <div className="message message--info">{readOnlyHint}</div> : null}
      {error ? <div className="message message--error">{error}</div> : null}

      <div className="form-actions">
        {canSubmit ? (
          <button className="button-primary" type="submit" disabled={isSubmitting}>
            {isSubmitting
              ? '提交中...'
              : submitLabel ?? (isCreateMode ? '创建任务' : '保存修改')}
          </button>
        ) : null}
        <button
          className="button-ghost"
          type="button"
          disabled={isSubmitting}
          onClick={handleReset}
        >
          {isCreateMode ? '清空草稿' : '恢复当前内容'}
        </button>
        {!isCreateMode && allowDelete && onDelete ? (
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
