import { useCallback, useEffect, useMemo, useState, type FormEvent } from 'react'
import { extractApiErrorCode, extractFieldErrorsFromApiError, hasFieldErrors, resolveUserMessage, type FieldErrors } from '../lib/apiError'
import {
  addTeamMember,
  createTeam,
  fetchAvailableUsers,
  fetchTeamDetail,
  fetchTeamMembers,
  fetchTeams,
  removeTeamMember,
  updateTeamMemberRole,
} from '../lib/teamApi'
import type { AvailableUser, TeamDetail, TeamMember, TeamRole, TeamSummary } from '../types/team'

type EditableTeamRole = Exclude<TeamRole, 'OWNER'>

type CreateTeamForm = {
  name: string
  description: string
}

const DEFAULT_CREATE_TEAM_FORM: CreateTeamForm = {
  name: '',
  description: '',
}

/**
 * チーム状態hookが必要とする認証状態、選択中チーム、画面遷移、共通メッセージ操作。
 */
type Params = {
  isLoggedIn: boolean
  selectedTeamId: string | null
  currentUserId: number | null
  go: (path: string, replace?: boolean) => void
  resetMessages: () => void
  setGlobalSuccessMessage: (value: string) => void
}

/**
 * チーム詳細取得失敗時のAPIエラーを、手順書指定の画面メッセージへ変換する。
 */
function mapTeamDetailError(error: unknown) {
  const code = extractApiErrorCode(error)

  if (code === 'ERR-TEAM-003') {
    return 'このチームにアクセスする権限がありません'
  }

  if (code === 'ERR-TEAM-004') {
    return '対象のチームが存在しません'
  }

  return resolveUserMessage(error)
}

/**
 * select値として保持したIDをAPIリクエスト用の数値へ変換する。
 */
function toNumericId(value: string) {
  const parsed = Number(value)
  return Number.isFinite(parsed) ? parsed : value
}

/**
 * チーム一覧、詳細、メンバー管理操作をまとめて管理する。
 */
export function useTeamState({
  isLoggedIn,
  selectedTeamId,
  currentUserId,
  go,
  resetMessages,
  setGlobalSuccessMessage,
}: Params) {
  const [teams, setTeams] = useState<TeamSummary[]>([])
  const [isLoadingTeams, setIsLoadingTeams] = useState(false)
  const [hasLoadedTeams, setHasLoadedTeams] = useState(false)
  const [teamErrorMessage, setTeamErrorMessage] = useState('')

  const [selectedTeam, setSelectedTeam] = useState<TeamDetail | null>(null)
  const [members, setMembers] = useState<TeamMember[]>([])
  const [isLoadingTeamDetail, setIsLoadingTeamDetail] = useState(false)
  const [isLoadingMembers, setIsLoadingMembers] = useState(false)
  const [teamDetailErrorMessage, setTeamDetailErrorMessage] = useState('')
  const [memberListErrorMessage, setMemberListErrorMessage] = useState('')

  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false)
  const [createForm, setCreateForm] = useState<CreateTeamForm>(DEFAULT_CREATE_TEAM_FORM)
  const [createFieldErrors, setCreateFieldErrors] = useState<FieldErrors>({})
  const [createErrorMessage, setCreateErrorMessage] = useState('')
  const [isCreatingTeam, setIsCreatingTeam] = useState(false)

  const [isAddMemberModalOpen, setIsAddMemberModalOpen] = useState(false)
  const [availableUsers, setAvailableUsers] = useState<AvailableUser[]>([])
  const [selectedUserId, setSelectedUserId] = useState('')
  const [selectedAddRole, setSelectedAddRole] = useState<EditableTeamRole>('MEMBER')
  const [addMemberFieldErrors, setAddMemberFieldErrors] = useState<FieldErrors>({})
  const [addMemberErrorMessage, setAddMemberErrorMessage] = useState('')
  const [availableUsersErrorMessage, setAvailableUsersErrorMessage] = useState('')
  const [isLoadingAvailableUsers, setIsLoadingAvailableUsers] = useState(false)
  const [isAddingMember, setIsAddingMember] = useState(false)

  const [changeRoleTarget, setChangeRoleTarget] = useState<TeamMember | null>(null)
  const [selectedChangeRole, setSelectedChangeRole] = useState<EditableTeamRole>('MEMBER')
  const [changeRoleFieldErrors, setChangeRoleFieldErrors] = useState<FieldErrors>({})
  const [changeRoleErrorMessage, setChangeRoleErrorMessage] = useState('')
  const [isChangingRole, setIsChangingRole] = useState(false)

  const [removeMemberTarget, setRemoveMemberTarget] = useState<TeamMember | null>(null)
  const [removeMemberErrorMessage, setRemoveMemberErrorMessage] = useState('')
  const [isRemovingMember, setIsRemovingMember] = useState(false)

  /**
   * 所属チーム一覧を取得する。
   */
  const loadTeams = useCallback(async () => {
    setIsLoadingTeams(true)
    setTeamErrorMessage('')

    try {
      const teamList = await fetchTeams()
      setTeams(teamList)
    } catch (error) {
      setTeams([])
      setTeamErrorMessage(resolveUserMessage(error))
    } finally {
      setHasLoadedTeams(true)
      setIsLoadingTeams(false)
    }
  }, [])

  /**
   * 指定チームのメンバー一覧を取得する。
   */
  const loadMembers = useCallback(async (teamId: string) => {
    setIsLoadingMembers(true)
    setMemberListErrorMessage('')

    try {
      const nextMembers = await fetchTeamMembers(teamId)
      setMembers(nextMembers)
    } catch {
      setMembers([])
      setMemberListErrorMessage('メンバー一覧の取得に失敗しました。再読み込みしてください')
    } finally {
      setIsLoadingMembers(false)
    }
  }, [])

  /**
   * 選択中チームの基本情報とメンバー一覧を取得する。
   */
  const loadSelectedTeam = useCallback(async (teamId = selectedTeamId) => {
    if (!teamId) {
      setSelectedTeam(null)
      setMembers([])
      return
    }

    setIsLoadingTeamDetail(true)
    setTeamDetailErrorMessage('')

    try {
      const team = await fetchTeamDetail(teamId)
      if (!team) {
        setSelectedTeam(null)
        setMembers([])
        setTeamDetailErrorMessage('対象のチームが存在しません')
        return
      }

      setSelectedTeam(team)
      await loadMembers(teamId)
    } catch (error) {
      setSelectedTeam(null)
      setMembers([])
      setTeamDetailErrorMessage(mapTeamDetailError(error))
    } finally {
      setIsLoadingTeamDetail(false)
    }
  }, [loadMembers, selectedTeamId])

  /**
   * チームへ追加可能なユーザーを取得する。
   */
  const loadAvailableUsers = useCallback(async (teamId = selectedTeamId) => {
    if (!teamId) {
      setAvailableUsers([])
      return
    }

    setIsLoadingAvailableUsers(true)
    setAvailableUsersErrorMessage('')

    try {
      const users = await fetchAvailableUsers(teamId)
      setAvailableUsers(users)
      setSelectedUserId((current) => {
        if (current && users.some((user) => String(user.userId) === current)) {
          return current
        }

        return ''
      })
    } catch (error) {
      setAvailableUsers([])
      setAvailableUsersErrorMessage(resolveUserMessage(error))
    } finally {
      setIsLoadingAvailableUsers(false)
    }
  }, [selectedTeamId])

  /**
   * チーム作成フォームをクライアント側で検証する。
   */
  const validateCreateForm = () => {
    const next: FieldErrors = {}

    if (!createForm.name.trim()) {
      next.name = 'チーム名を入力してください。'
    } else if (createForm.name.trim().length > 100) {
      next.name = 'チーム名は100文字以内で入力してください。'
    }

    if (createForm.description.length > 1000) {
      next.description = 'チーム説明は1000文字以内で入力してください。'
    }

    return next
  }

  /**
   * メンバー追加フォームをクライアント側で検証する。
   */
  const validateAddMemberForm = () => {
    const next: FieldErrors = {}

    if (!selectedUserId) {
      next.userId = '追加するユーザーを選択してください。'
    }

    if (selectedAddRole !== 'ADMIN' && selectedAddRole !== 'MEMBER') {
      next.role = 'ロールを選択してください。'
    }

    return next
  }

  /**
   * チーム作成モーダルを開く。
   */
  const openCreateTeamModal = () => {
    resetMessages()
    setCreateForm(DEFAULT_CREATE_TEAM_FORM)
    setCreateFieldErrors({})
    setCreateErrorMessage('')
    setIsCreateModalOpen(true)
  }

  /**
   * チーム作成モーダルを閉じる。
   */
  const closeCreateTeamModal = () => {
    if (isCreatingTeam) {
      return
    }

    setIsCreateModalOpen(false)
  }

  /**
   * チーム作成を実行し、作成したチーム詳細へ遷移する。
   */
  const handleCreateTeam = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    resetMessages()
    setCreateErrorMessage('')
    setCreateFieldErrors({})

    const validationErrors = validateCreateForm()
    if (hasFieldErrors(validationErrors)) {
      setCreateFieldErrors(validationErrors)
      setCreateErrorMessage('入力内容を確認してください。')
      return
    }

    setIsCreatingTeam(true)
    try {
      const createdTeam = await createTeam({
        name: createForm.name.trim(),
        description: createForm.description.trim() || undefined,
      })
      setCreateForm(DEFAULT_CREATE_TEAM_FORM)
      setIsCreateModalOpen(false)
      await loadTeams()
      go(`/teams/${createdTeam.id}`)
      setGlobalSuccessMessage('チームを作成しました。')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setCreateFieldErrors(apiFieldErrors)
      }
      setCreateErrorMessage(resolveUserMessage(error))
    } finally {
      setIsCreatingTeam(false)
    }
  }

  /**
   * メンバー追加モーダルを開き、追加候補ユーザーを取得する。
   */
  const openAddMemberModal = () => {
    resetMessages()
    setSelectedUserId('')
    setSelectedAddRole('MEMBER')
    setAddMemberFieldErrors({})
    setAddMemberErrorMessage('')
    setAvailableUsers([])
    setIsAddMemberModalOpen(true)
    void loadAvailableUsers()
  }

  /**
   * メンバー追加モーダルを閉じる。
   */
  const closeAddMemberModal = () => {
    if (isAddingMember) {
      return
    }

    setIsAddMemberModalOpen(false)
  }

  /**
   * 選択中チームへメンバーを追加する。
   */
  const handleAddMember = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedTeamId) {
      return
    }

    resetMessages()
    setAddMemberErrorMessage('')
    setAddMemberFieldErrors({})

    const validationErrors = validateAddMemberForm()
    if (hasFieldErrors(validationErrors)) {
      setAddMemberFieldErrors(validationErrors)
      setAddMemberErrorMessage('入力内容を確認してください。')
      return
    }

    setIsAddingMember(true)
    try {
      await addTeamMember(selectedTeamId, {
        userId: toNumericId(selectedUserId),
        role: selectedAddRole,
      })
      setIsAddMemberModalOpen(false)
      await Promise.all([loadSelectedTeam(selectedTeamId), loadAvailableUsers(selectedTeamId)])
      setGlobalSuccessMessage('メンバーを追加しました。')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setAddMemberFieldErrors(apiFieldErrors)
      }
      setAddMemberErrorMessage(resolveUserMessage(error))
    } finally {
      setIsAddingMember(false)
    }
  }

  /**
   * ロール変更モーダルを開く。
   */
  const openChangeRoleModal = (member: TeamMember) => {
    resetMessages()
    setChangeRoleTarget(member)
    setSelectedChangeRole(member.role === 'ADMIN' ? 'ADMIN' : 'MEMBER')
    setChangeRoleFieldErrors({})
    setChangeRoleErrorMessage('')
  }

  /**
   * ロール変更モーダルを閉じる。
   */
  const closeChangeRoleModal = () => {
    if (isChangingRole) {
      return
    }

    setChangeRoleTarget(null)
  }

  /**
   * チームメンバーのロールを変更する。
   */
  const handleChangeRole = async (event: FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!selectedTeamId || !changeRoleTarget) {
      return
    }

    resetMessages()
    setChangeRoleErrorMessage('')
    setChangeRoleFieldErrors({})

    if (selectedChangeRole !== 'ADMIN' && selectedChangeRole !== 'MEMBER') {
      setChangeRoleFieldErrors({ role: 'ロールを選択してください。' })
      setChangeRoleErrorMessage('入力内容を確認してください。')
      return
    }

    setIsChangingRole(true)
    try {
      await updateTeamMemberRole(selectedTeamId, changeRoleTarget.memberId, {
        role: selectedChangeRole,
      })
      setChangeRoleTarget(null)
      await loadSelectedTeam(selectedTeamId)
      setGlobalSuccessMessage('ロールを変更しました。')
    } catch (error) {
      const apiFieldErrors = extractFieldErrorsFromApiError(error)
      if (hasFieldErrors(apiFieldErrors)) {
        setChangeRoleFieldErrors(apiFieldErrors)
      }
      setChangeRoleErrorMessage(resolveUserMessage(error))
    } finally {
      setIsChangingRole(false)
    }
  }

  /**
   * メンバー削除確認ダイアログを開く。
   */
  const openRemoveMemberDialog = (member: TeamMember) => {
    resetMessages()
    setRemoveMemberTarget(member)
    setRemoveMemberErrorMessage('')
  }

  /**
   * メンバー削除確認ダイアログを閉じる。
   */
  const closeRemoveMemberDialog = () => {
    if (isRemovingMember) {
      return
    }

    setRemoveMemberTarget(null)
  }

  /**
   * チームメンバーを削除する。
   */
  const handleRemoveMember = async () => {
    if (!selectedTeamId || !removeMemberTarget) {
      return
    }

    resetMessages()
    setRemoveMemberErrorMessage('')
    setIsRemovingMember(true)

    try {
      const isSelfRemove = currentUserId !== null && String(removeMemberTarget.userId) === String(currentUserId)
      await removeTeamMember(selectedTeamId, removeMemberTarget.memberId)
      setRemoveMemberTarget(null)

      if (isSelfRemove) {
        setSelectedTeam(null)
        setMembers([])
        await loadTeams()
        go('/teams')
        setGlobalSuccessMessage('チームから外れました')
        return
      }

      await loadSelectedTeam(selectedTeamId)
      setGlobalSuccessMessage('メンバーを削除しました。')
    } catch (error) {
      setRemoveMemberErrorMessage(resolveUserMessage(error))
    } finally {
      setIsRemovingMember(false)
    }
  }

  useEffect(() => {
    if (!isLoggedIn) {
      setTeams([])
      setTeamErrorMessage('')
      setIsLoadingTeams(false)
      setHasLoadedTeams(false)
      return
    }

    void loadTeams()
  }, [isLoggedIn, loadTeams])

  useEffect(() => {
    if (!isLoggedIn) {
      setSelectedTeam(null)
      setMembers([])
      setTeamDetailErrorMessage('')
      setMemberListErrorMessage('')
      setIsLoadingTeamDetail(false)
      setIsLoadingMembers(false)
      return
    }

    if (selectedTeamId) {
      void loadSelectedTeam(selectedTeamId)
    } else {
      setSelectedTeam(null)
      setMembers([])
      setTeamDetailErrorMessage('')
      setMemberListErrorMessage('')
    }
  }, [isLoggedIn, loadSelectedTeam, selectedTeamId])

  /**
   * ログアウト時にチーム関連状態をすべて初期化する。
   */
  const clearTeamStateOnLogout = () => {
    setTeams([])
    setTeamErrorMessage('')
    setIsLoadingTeams(false)
    setHasLoadedTeams(false)
    setSelectedTeam(null)
    setMembers([])
    setTeamDetailErrorMessage('')
    setMemberListErrorMessage('')
    setIsLoadingTeamDetail(false)
    setIsLoadingMembers(false)
    setIsCreateModalOpen(false)
    setCreateForm(DEFAULT_CREATE_TEAM_FORM)
    setCreateFieldErrors({})
    setCreateErrorMessage('')
    setIsCreatingTeam(false)
    setIsAddMemberModalOpen(false)
    setAvailableUsers([])
    setSelectedUserId('')
    setSelectedAddRole('MEMBER')
    setAddMemberFieldErrors({})
    setAddMemberErrorMessage('')
    setAvailableUsersErrorMessage('')
    setIsLoadingAvailableUsers(false)
    setIsAddingMember(false)
    setChangeRoleTarget(null)
    setSelectedChangeRole('MEMBER')
    setChangeRoleFieldErrors({})
    setChangeRoleErrorMessage('')
    setIsChangingRole(false)
    setRemoveMemberTarget(null)
    setRemoveMemberErrorMessage('')
    setIsRemovingMember(false)
  }

  const createTeamForm = useMemo(
    () => ({
      ...createForm,
      fieldErrors: createFieldErrors,
      onNameChange: (value: string) => {
        setCreateForm((current) => ({ ...current, name: value }))
        setCreateFieldErrors((current) => {
          const next = { ...current }
          delete next.name
          return next
        })
      },
      onDescriptionChange: (value: string) => {
        setCreateForm((current) => ({ ...current, description: value }))
        setCreateFieldErrors((current) => {
          const next = { ...current }
          delete next.description
          return next
        })
      },
    }),
    [createFieldErrors, createForm],
  )

  return {
    list: {
      teams,
      isLoadingTeams,
      hasLoadedTeams,
      teamErrorMessage,
      loadTeams,
    },
    detail: {
      selectedTeam,
      members,
      isLoadingTeamDetail,
      isLoadingMembers,
      teamDetailErrorMessage,
      memberListErrorMessage,
      loadSelectedTeam,
      loadMembers,
    },
    create: {
      isOpen: isCreateModalOpen,
      form: createTeamForm,
      errorMessage: createErrorMessage,
      isSubmitting: isCreatingTeam,
    },
    addMember: {
      isOpen: isAddMemberModalOpen,
      availableUsers,
      selectedUserId,
      selectedRole: selectedAddRole,
      fieldErrors: addMemberFieldErrors,
      errorMessage: addMemberErrorMessage,
      availableUsersError: availableUsersErrorMessage,
      isLoadingAvailableUsers,
      isSubmitting: isAddingMember,
    },
    changeRole: {
      targetMember: changeRoleTarget,
      selectedRole: selectedChangeRole,
      fieldErrors: changeRoleFieldErrors,
      errorMessage: changeRoleErrorMessage,
      isSubmitting: isChangingRole,
    },
    removeMember: {
      targetMember: removeMemberTarget,
      errorMessage: removeMemberErrorMessage,
      isSubmitting: isRemovingMember,
    },
    actions: {
      openCreateTeamModal,
      closeCreateTeamModal,
      handleCreateTeam,
      handleShowTeamDetail: (teamId: number | string) => go(`/teams/${teamId}`),
      openAddMemberModal,
      closeAddMemberModal,
      setSelectedAddUserId: (value: string) => {
        setSelectedUserId(value)
        setAddMemberFieldErrors((current) => {
          const next = { ...current }
          delete next.userId
          return next
        })
      },
      setSelectedAddRole: (value: EditableTeamRole) => {
        setSelectedAddRole(value)
        setAddMemberFieldErrors((current) => {
          const next = { ...current }
          delete next.role
          return next
        })
      },
      handleAddMember,
      reloadAvailableUsers: () => void loadAvailableUsers(),
      openChangeRoleModal,
      closeChangeRoleModal,
      setSelectedChangeRole: (value: EditableTeamRole) => {
        setSelectedChangeRole(value)
        setChangeRoleFieldErrors((current) => {
          const next = { ...current }
          delete next.role
          return next
        })
      },
      handleChangeRole,
      openRemoveMemberDialog,
      closeRemoveMemberDialog,
      handleRemoveMember,
      showTeamListError: (message: string) => setTeamErrorMessage(message),
      clearTeamStateOnLogout,
    },
  }
}

export type UseTeamStateResult = ReturnType<typeof useTeamState>
