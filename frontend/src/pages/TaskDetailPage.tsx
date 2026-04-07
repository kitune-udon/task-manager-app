import { TaskShell } from '../components/TaskShell'
import type { TaskItem } from '../lib/taskApi'
import { formatDate } from '../utils/format'

type Props = {
  activePath: string
  currentUserLabel: string
  onNavigate: (path: string) => void
  onLogout: () => void
  onShowList: () => void
  onShowEdit: () => void
  onDelete: () => void
  isDeleting: boolean
  detailErrorMessage: string
  successMessage: string
  isLoadingDetail: boolean
  selectedTask: TaskItem | null
  commentDraft: string
  onCommentDraftChange: (value: string) => void
}

export function TaskDetailPage(props: Props) {
  const { selectedTask } = props

  return (
    <TaskShell
      title="タスク詳細"
      description="タスクの内容、コメント、添付ファイルを確認します"
      activePath={props.activePath}
      onNavigate={props.onNavigate}
      onLogout={props.onLogout}
      currentUserLabel={props.currentUserLabel}
      actions={
        <>
          <button className="secondary-button" onClick={props.onShowList} type="button">一覧へ戻る</button>
          <button className="secondary-button" onClick={props.onShowEdit} type="button">編集</button>
          <button className="primary-button danger-button" disabled={props.isDeleting} onClick={props.onDelete} type="button">
            {props.isDeleting ? '削除中...' : '削除'}
          </button>
        </>
      }
    >
      {props.detailErrorMessage ? <div className="status-box error-box">{props.detailErrorMessage}</div> : null}
      {props.successMessage ? <div className="status-box success-box">{props.successMessage}</div> : null}

      <section className="panel section-panel detail-layout-panel">
        {props.isLoadingDetail ? <p className="empty-message">タスクを読み込み中です...</p> : !selectedTask ? (
          <p className="empty-message">タスクが見つかりません。</p>
        ) : (
          <div className="task-detail-layout">
            <section className="task-detail-main-card">
              <h2 className="task-detail-title">{selectedTask.title}</h2>
              <div className="task-detail-meta-grid">
                <div className="task-detail-meta-item"><p className="summary-label">ステータス</p><strong>{selectedTask.status ?? '-'}</strong></div>
                <div className="task-detail-meta-item"><p className="summary-label">優先度</p><strong>{selectedTask.priority ?? '-'}</strong></div>
                <div className="task-detail-meta-item"><p className="summary-label">期限</p><strong>{formatDate(selectedTask.dueDate)}</strong></div>
                <div className="task-detail-meta-item"><p className="summary-label">担当者</p><strong>{selectedTask.assignedUserName ?? '-'}</strong></div>
                <div className="task-detail-meta-item"><p className="summary-label">作成日</p><strong>{formatDate(selectedTask.createdAt)}</strong></div>
                <div className="task-detail-meta-item"><p className="summary-label">更新日</p><strong>{formatDate(selectedTask.updatedAt)}</strong></div>
                <div className="task-detail-meta-item full"><p className="summary-label">作成者</p><strong>{selectedTask.createdByName ?? '-'}</strong></div>
              </div>
              <div className="task-detail-description-block">
                <p className="summary-label">説明</p>
                <div className="task-detail-description-box">{selectedTask.description ?? '-'}</div>
              </div>
            </section>

            <aside className="task-detail-side-column">
              <section className="task-side-card">
                <h3>コメント</h3>
                <div className="comment-item">
                  <p className="comment-author">{selectedTask.createdByName ?? props.currentUserLabel ?? 'ログインユーザー'}</p>
                  <p className="comment-text">{selectedTask.description ? `${selectedTask.description.slice(0, 40)}${selectedTask.description.length > 40 ? '…' : ''}` : 'コメントはまだありません。'}</p>
                </div>
                <textarea className="comment-input" value={props.commentDraft} onChange={(e) => props.onCommentDraftChange(e.target.value)} placeholder="コメントを入力" rows={2} />
                <button className="primary-button side-card-button" type="button">コメント投稿</button>
              </section>

              <section className="task-side-card">
                <h3>添付ファイル</h3>
                <div className="file-item">
                  <span>{`task-${selectedTask.id}.xlsx`}</span>
                  <button className="link-button" type="button">DL</button>
                </div>
                <button className="secondary-button side-card-button" type="button">ファイルを添付</button>
              </section>
            </aside>
          </div>
        )}
      </section>
    </TaskShell>
  )
}
