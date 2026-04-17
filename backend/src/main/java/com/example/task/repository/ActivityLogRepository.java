package com.example.task.repository;

import com.example.task.entity.ActivityEventType;
import com.example.task.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * アクティビティログのタスク別一覧取得を扱うリポジトリ。
 */
public interface ActivityLogRepository extends JpaRepository<ActivityLog, Long> {

    @EntityGraph(attributePaths = {"actorUser"})
    @Query("""
            select a
            from ActivityLog a
            where a.task.id = :taskId
              and (:eventType is null or a.eventType = :eventType)
            order by a.createdAt desc, a.id desc
            """)
    Page<ActivityLog> findByTaskId(
            @Param("taskId") Long taskId,
            @Param("eventType") ActivityEventType eventType,
            Pageable pageable
    );
}
