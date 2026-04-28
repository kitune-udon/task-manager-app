package com.example.task.dto.team;

import com.example.task.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 所属チーム一覧 API が返すレスポンス DTO。
 */
@Getter
@Builder
public class TeamSummaryResponse {
    private Long id;
    private String name;
    private String description;
    private TeamRole myRole;
    private long memberCount;
    private LocalDateTime updatedAt;
}
