import type { FormEvent } from 'react'
import type { FieldErrors } from '../../lib/apiError'

type CreateTeamForm = {
  name: string
  description: string
}

/**
 * チーム作成モーダルに表示するフォーム状態と操作。
 */
type Props = {
  isOpen: boolean
  form: CreateTeamForm
  fieldErrors: FieldErrors
  errorMessage: string
  isSubmitting: boolean
  onNameChange: (value: string) => void
  onDescriptionChange: (value: string) => void
  onSubmit: (event: FormEvent<HTMLFormElement>) => void
  onClose: () => void
}

/**
 * チーム名と説明を入力して新しいチームを作成するモーダル。
 */
export function CreateTeamModal({
  isOpen,
  form,
  fieldErrors,
  errorMessage,
  isSubmitting,
  onNameChange,
  onDescriptionChange,
  onSubmit,
  onClose,
}: Props) {
  if (!isOpen) {
    return null
  }

  return (
    <div className="modal-backdrop" role="presentation">
      <section aria-modal="true" className="modal-panel" role="dialog">
        <header className="modal-header">
          <div>
            <h2>チーム作成</h2>
            <p className="subtext">作成者は自動的に OWNER として所属します。</p>
          </div>
          <button className="text-link-button modal-close-button" disabled={isSubmitting} onClick={onClose} type="button">
            閉じる
          </button>
        </header>

        {errorMessage ? <div className="status-box error-box">{errorMessage}</div> : null}

        <form className="form-grid auth-form" onSubmit={onSubmit}>
          <label>
            <span>
              チーム名 <em className="required-mark">必須</em>
            </span>
            <input
              className={fieldErrors.name ? 'input-error' : ''}
              maxLength={100}
              onChange={(event) => onNameChange(event.target.value)}
              type="text"
              value={form.name}
            />
            <span className="character-count">{form.name.length} / 100</span>
            {fieldErrors.name ? <span className="field-error">{fieldErrors.name}</span> : null}
          </label>

          <label>
            <span>チーム説明</span>
            <textarea
              className={fieldErrors.description ? 'input-error' : ''}
              maxLength={1000}
              onChange={(event) => onDescriptionChange(event.target.value)}
              rows={5}
              value={form.description}
            />
            <span className="character-count">{form.description.length} / 1000</span>
            {fieldErrors.description ? <span className="field-error">{fieldErrors.description}</span> : null}
          </label>

          <div className="form-actions split-actions">
            <button className="secondary-button" disabled={isSubmitting} onClick={onClose} type="button">
              キャンセル
            </button>
            <button className="primary-button" disabled={isSubmitting} type="submit">
              {isSubmitting ? '作成中...' : '作成する'}
            </button>
          </div>
        </form>
      </section>
    </div>
  )
}
