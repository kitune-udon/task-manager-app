package com.example.task.repository;

import com.example.task.entity.Priority;
import com.example.task.entity.Task;
import com.example.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TaskRepository extends JpaRepository<Task, Long> {

    @EntityGraph(attributePaths = {"assignedUser"})
    @Query("""
            select t
            from Task t
            where (:status is null or t.status = :status)
              and (:priority is null or t.priority = :priority)
              and (:assignedUserId is null or t.assignedUser.id = :assignedUserId)
              and (:keywordPattern is null or lower(t.title) like :keywordPattern)
            order by t.createdAt desc, t.id desc
            """)
    List<Task> search(
            @Param("status") TaskStatus status,
            @Param("priority") Priority priority,
            @Param("assignedUserId") Long assignedUserId,
            @Param("keywordPattern") String keywordPattern
    );

    @EntityGraph(attributePaths = {"assignedUser"})
    Optional<Task> findWithAssignedUserById(Long id);
}