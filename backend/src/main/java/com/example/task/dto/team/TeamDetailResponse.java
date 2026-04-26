package com.example.task.dto.team;

import com.example.task.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * チーム詳細 API が返すレスポンス DTO。
 */
@Getter
@Builder
public class TeamDetailResponse {
    private Long id;
    private String name;
    private String description;
    private TeamRole myRole;
    private long memberCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
