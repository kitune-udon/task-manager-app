package com.example.task.dto.team;

import com.example.task.entity.TeamRole;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * チームメンバー追加 API の入力値を受け取る DTO。
 */
@Getter
@Setter
public class AddTeamMemberRequest {

    @NotNull(message = "追加するユーザーを選択してください")
    private Long userId;

    @NotNull(message = "ロールを選択してください")
    private TeamRole role;
}
