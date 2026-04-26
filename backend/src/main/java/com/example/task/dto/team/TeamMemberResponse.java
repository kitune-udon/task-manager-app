package com.example.task.dto.team;

import com.example.task.entity.TeamRole;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * チームメンバー API が返すレスポンス DTO。
 */
@Getter
@Builder
public class TeamMemberResponse {
    private Long memberId;
    private Long userId;
    private String name;
    private String email;
    private TeamRole role;
    private LocalDateTime joinedAt;
}
