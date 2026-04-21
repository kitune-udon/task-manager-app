package com.example.task.service;

import com.example.task.entity.Task;
import com.example.task.entity.User;
import com.example.task.exception.BusinessException;
import com.example.task.exception.ErrorCode;
import com.example.task.exception.ResourceNotFoundException;
import com.example.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * タスクを親に持つ機能向けの存在確認と認可判定を共通化する。
 */
@Service
public class TaskAuthorizationService {

    private final TaskRepository taskRepository;

    /**
     * タスク認可サービスを生成する。
     *
     * @param taskRepository タスクリポジトリ
     */
    public TaskAuthorizationService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    /**
     * 削除されていないタスクを取得する。
     *
     * @param taskId タスクID
     * @return アクティブなタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     */
    @Transactional(readOnly = true)
    public Task getActiveTask(Long taskId) {
        return taskRepository.findWithAssignedUserByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません"));
    }

    /**
     * 現在のユーザーが参照可能なタスクを取得する。
     *
     * <p>タスク作成者または担当者の場合に参照を許可する。</p>
     *
     * @param taskId タスクID
     * @param currentUserId 現在のユーザーID
     * @return 参照可能なタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     * @throws BusinessException 参照権限がない場合
     */
    @Transactional(readOnly = true)
    public Task getViewableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeView(task, currentUserId);
        return task;
    }

    /**
     * 現在のユーザーが更新可能なタスクを取得する。
     *
     * <p>タスク作成者または担当者の場合に更新を許可する。</p>
     *
     * @param taskId タスクID
     * @param currentUserId 現在のユーザーID
     * @return 更新可能なタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     * @throws BusinessException 更新権限がない場合
     */
    @Transactional(readOnly = true)
    public Task getUpdatableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeUpdate(task, currentUserId);
        return task;
    }

    /**
     * 現在のユーザーが削除可能なタスクを取得する。
     *
     * <p>タスク作成者の場合に削除を許可する。</p>
     *
     * @param taskId タスクID
     * @param currentUserId 現在のユーザーID
     * @return 削除可能なタスク
     * @throws ResourceNotFoundException タスクが存在しない、または削除済みの場合
     * @throws BusinessException 削除権限がない場合
     */
    @Transactional(readOnly = true)
    public Task getDeletableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeDelete(task, currentUserId);
        return task;
    }

    /**
     * タスクの参照権限を検証する。
     *
     * @param task 検証対象のタスク
     * @param currentUserId 現在のユーザーID
     * @throws BusinessException タスク作成者または担当者ではない場合
     */
    public void authorizeView(Task task, Long currentUserId) {
        if (isTaskCreator(task, currentUserId) || isTaskAssignee(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.AUTH_005, "対象タスクの参照権限がありません");
    }

    /**
     * タスクの更新権限を検証する。
     *
     * @param task 検証対象のタスク
     * @param currentUserId 現在のユーザーID
     * @throws BusinessException タスク作成者または担当者ではない場合
     */
    public void authorizeUpdate(Task task, Long currentUserId) {
        if (isTaskCreator(task, currentUserId) || isTaskAssignee(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.PERM_TASK_403_UPD);
    }

    /**
     * タスクの削除権限を検証する。
     *
     * @param task 検証対象のタスク
     * @param currentUserId 現在のユーザーID
     * @throws BusinessException タスク作成者ではない場合
     */
    public void authorizeDelete(Task task, Long currentUserId) {
        if (isTaskCreator(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.PERM_TASK_403_DEL);
    }

    /**
     * 指定されたユーザーがタスク作成者かどうかを判定する。
     *
     * @param task 判定対象のタスク
     * @param currentUserId 現在のユーザーID
     * @return タスク作成者の場合はtrue
     */
    public boolean isTaskCreator(Task task, Long currentUserId) {
        User createdBy = task.getCreatedBy();
        return createdBy != null && currentUserId.equals(createdBy.getId());
    }

    /**
     * 指定されたユーザーがタスク担当者かどうかを判定する。
     *
     * @param task 判定対象のタスク
     * @param currentUserId 現在のユーザーID
     * @return タスク担当者の場合はtrue
     */
    public boolean isTaskAssignee(Task task, Long currentUserId) {
        User assignedUser = task.getAssignedUser();
        return assignedUser != null && currentUserId.equals(assignedUser.getId());
    }
}
