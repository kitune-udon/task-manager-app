import { useEffect, useMemo, useState } from 'react'
import { resolveUserMessage } from '../lib/authApi'
import type { TaskItem } from '../lib/taskApi'
import { fetchAssignableUsers } from '../lib/userApi'
import type { AssigneeOption } from './taskStateShared'

/**
 * 担当者候補を読み込むために必要な認証状態と、編集中タスクの現在値。
 */
type Params = {
  isLoggedIn: boolean
  selectedTask: TaskItem | null
}

/**
 * タスク作成・編集フォームで使う担当者候補の取得、選択肢生成、状態クリアをまとめる。
 */
export function useAssignableUsers({ isLoggedIn, selectedTask }: Params) {
  const [assignableUsers, setAssignableUsers] = useState<Array<{ id: number | string; name: string; email: string }>>([])
  const [isLoadingAssignableUsers, setIsLoadingAssignableUsers] = useState(false)
  const [assigneeOptionsError, setAssigneeOptionsError] = useState('')

  /**
   * APIから担当者候補を再取得する。
   */
  const loadAssignableUsers = async () => {
    setIsLoadingAssignableUsers(true)
    setAssigneeOptionsError('')

    try {
      const users = await fetchAssignableUsers()
      setAssignableUsers(users)
    } catch (error) {
      setAssignableUsers([])
      setAssigneeOptionsError(resolveUserMessage(error))
    } finally {
      setIsLoadingAssignableUsers(false)
    }
  }

  useEffect(() => {
    if (isLoggedIn) {
      void loadAssignableUsers()
    } else {
      // ログアウト後に前ユーザーの候補やエラー表示を残さない。
      setAssignableUsers([])
      setAssigneeOptionsError('')
      setIsLoadingAssignableUsers(false)
    }
  }, [isLoggedIn])

  const assigneeOptions = useMemo<AssigneeOption[]>(() => {
    const options = assignableUsers.map((user) => ({
      value: String(user.id),
      label: `${user.name} (${user.email})`,
    }))

    const selectedAssignedUserId =
      selectedTask?.assignedUserId !== undefined && selectedTask?.assignedUserId !== null
        ? String(selectedTask.assignedUserId)
        : null

    if (selectedAssignedUserId && !options.some((option) => option.value === selectedAssignedUserId)) {
      // 現在の担当者が候補APIに含まれない場合でも、編集フォーム上で既存値を表示できるようにする。
      options.unshift({
        value: selectedAssignedUserId,
        label: selectedTask?.assignedUserName ?? `ユーザーID: ${selectedAssignedUserId}`,
      })
    }

    return [{ label: '未選択', value: '' }, ...options]
  }, [assignableUsers, selectedTask?.assignedUserId, selectedTask?.assignedUserName])

  /**
   * 担当者候補の状態を初期化する。
   */
  const clearAssignableUsersState = () => {
    setAssignableUsers([])
    setAssigneeOptionsError('')
    setIsLoadingAssignableUsers(false)
  }

  return {
    assigneeOptions,
    assigneeOptionsError,
    isLoadingAssignableUsers,
    loadAssignableUsers,
    clearAssignableUsersState,
  }
}
