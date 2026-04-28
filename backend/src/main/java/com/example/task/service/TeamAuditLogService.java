package com.example.task.service;

import com.example.task.entity.TeamRole;
import com.example.task.logging.StructuredLogService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;

/**
 * チーム管理操作の監査ログを出力するサービス。
 */
@Service
public class TeamAuditLogService {

    private final StructuredLogService structuredLogService;

    public TeamAuditLogService(StructuredLogService structuredLogService) {
        this.structuredLogService = structuredLogService;
    }

    public void logTeamCreated(Long teamId, String teamName) {
        LinkedHashMap<String, Object> fields = baseFields(HttpStatus.CREATED.value(), teamId);
        fields.put("teamName", teamName);
        structuredLogService.infoAudit("LOG-TEAM-001", "チーム作成成功", fields);
    }

    public void logMemberAdded(Long teamId, Long memberUserId, TeamRole actorRole, TeamRole newRole) {
        LinkedHashMap<String, Object> fields = memberFields(HttpStatus.CREATED.value(), teamId, memberUserId, actorRole);
        fields.put("newRole", newRole);
        structuredLogService.infoAudit("LOG-TEAM-002", "チームメンバー追加成功", fields);
    }

    public void logMemberRoleUpdated(
            Long teamId,
            Long memberUserId,
            TeamRole actorRole,
            TeamRole previousRole,
            TeamRole newRole
    ) {
        LinkedHashMap<String, Object> fields = memberFields(HttpStatus.OK.value(), teamId, memberUserId, actorRole);
        fields.put("previousRole", previousRole);
        fields.put("targetRole", newRole);
        fields.put("newRole", newRole);
        structuredLogService.infoAudit("LOG-TEAM-003", "チームメンバーのロール変更成功", fields);
    }

    public void logMemberRemoved(Long teamId, Long memberUserId, TeamRole actorRole) {
        LinkedHashMap<String, Object> fields = memberFields(HttpStatus.NO_CONTENT.value(), teamId, memberUserId, actorRole);
        structuredLogService.infoAudit("LOG-TEAM-004", "チームメンバー削除成功", fields);
    }

    private LinkedHashMap<String, Object> baseFields(int status, Long teamId) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(status, true, false, false);
        fields.put("teamId", teamId);
        return fields;
    }

    private LinkedHashMap<String, Object> memberFields(
            int status,
            Long teamId,
            Long memberUserId,
            TeamRole actorRole
    ) {
        LinkedHashMap<String, Object> fields = baseFields(status, teamId);
        fields.put("memberUserId", memberUserId);
        fields.put("actorRole", actorRole);
        return fields;
    }
}
