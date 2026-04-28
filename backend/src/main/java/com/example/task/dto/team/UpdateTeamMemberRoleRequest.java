package com.example.task.dto.team;

import com.example.task.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * チームメンバーのロール変更 API の入力値を受け取る DTO。
 */
@Getter
@Setter
public class UpdateTeamMemberRoleRequest {

    @NotNull(message = "ロールを選択してください")
    private TeamRole role;
}
