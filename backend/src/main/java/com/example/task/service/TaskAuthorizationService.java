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

    public TaskAuthorizationService(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Transactional(readOnly = true)
    public Task getActiveTask(Long taskId) {
        return taskRepository.findWithAssignedUserByIdAndDeletedAtIsNull(taskId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RES_TASK_404, "タスクが存在しません"));
    }

    @Transactional(readOnly = true)
    public Task getViewableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeView(task, currentUserId);
        return task;
    }

    @Transactional(readOnly = true)
    public Task getUpdatableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeUpdate(task, currentUserId);
        return task;
    }

    @Transactional(readOnly = true)
    public Task getDeletableTask(Long taskId, Long currentUserId) {
        Task task = getActiveTask(taskId);
        authorizeDelete(task, currentUserId);
        return task;
    }

    public void authorizeView(Task task, Long currentUserId) {
        if (isTaskCreator(task, currentUserId) || isTaskAssignee(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.AUTH_005, "対象タスクの参照権限がありません");
    }

    public void authorizeUpdate(Task task, Long currentUserId) {
        if (isTaskCreator(task, currentUserId) || isTaskAssignee(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.PERM_TASK_403_UPD);
    }

    public void authorizeDelete(Task task, Long currentUserId) {
        if (isTaskCreator(task, currentUserId)) {
            return;
        }

        throw new BusinessException(ErrorCode.PERM_TASK_403_DEL);
    }

    public boolean isTaskCreator(Task task, Long currentUserId) {
        User createdBy = task.getCreatedBy();
        return createdBy != null && currentUserId.equals(createdBy.getId());
    }

    public boolean isTaskAssignee(Task task, Long currentUserId) {
        User assignedUser = task.getAssignedUser();
        return assignedUser != null && currentUserId.equals(assignedUser.getId());
    }
}
