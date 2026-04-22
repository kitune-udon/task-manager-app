package com.example.task.logging;

import org.slf4j.event.Level;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 設計書に定義した application / security / audit ログを共通形式で出力する。
 */
@Service
public class StructuredLogService {

    private static final String SERVICE_NAME = "task-app";

    private final Environment environment;
    private final RequestLogContext requestLogContext;
    private final String version;

    /**
     * 構造化ログサービスを生成する。
     *
     * @param environment Spring環境情報
     * @param requestLogContext リクエストログコンテキスト
     * @param buildPropertiesProvider ビルド情報プロバイダー
     */
    public StructuredLogService(
            Environment environment,
            RequestLogContext requestLogContext,
            ObjectProvider<BuildProperties> buildPropertiesProvider
    ) {
        this.environment = environment;
        this.requestLogContext = requestLogContext;
        BuildProperties buildProperties = buildPropertiesProvider.getIfAvailable();
        // Spring Boot の BuildProperties が利用できない実行形態では、クラスパス上のbuild-infoを直接読む。
        this.version = buildProperties != null
                ? buildProperties.getVersion()
                : BuildInfoResourceLoader.loadVersion();
    }

    /**
     * applicationチャンネルへDEBUGログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void debugApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.DEBUG, eventId, message, fields);
    }

    /**
     * applicationチャンネルへINFOログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void infoApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.INFO, eventId, message, fields);
    }

    /**
     * applicationチャンネルへWARNログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void warnApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.WARN, eventId, message, fields);
    }

    /**
     * applicationチャンネルへERRORログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void errorApplication(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.APPLICATION, Level.ERROR, eventId, message, fields);
    }

    /**
     * securityチャンネルへINFOログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void infoSecurity(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.SECURITY, Level.INFO, eventId, message, fields);
    }

    /**
     * securityチャンネルへWARNログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void warnSecurity(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.SECURITY, Level.WARN, eventId, message, fields);
    }

    /**
     * auditチャンネルへINFOログを出力する。
     *
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    public void infoAudit(String eventId, String message, Map<String, Object> fields) {
        log(LogChannel.AUDIT, Level.INFO, eventId, message, fields);
    }

    /**
     * HTTPリクエストに紐づく共通ログフィールドを生成する。
     *
     * @param request HTTPリクエスト
     * @param status HTTPステータスコード
     * @param includeUserId ユーザーIDを含める場合はtrue
     * @param includeIp クライアントIPを含める場合はtrue
     * @param includeDuration 処理時間を含める場合はtrue
     * @return リクエストログ用のフィールド
     */
    public LinkedHashMap<String, Object> requestFields(
            HttpServletRequest request,
            Integer status,
            boolean includeUserId,
            boolean includeIp,
            boolean includeDuration
    ) {
        LinkedHashMap<String, Object> fields = new LinkedHashMap<>();
        // null値はputIfPresentで除外し、ログに不要なキーを出さない。
        putIfPresent(fields, "requestId", requestLogContext.getRequestId(request));
        putIfPresent(fields, "userId", includeUserId ? requestLogContext.getAuthenticatedUserId() : null);
        putIfPresent(fields, "path", request.getRequestURI());
        putIfPresent(fields, "method", request.getMethod());
        putIfPresent(fields, "status", status);
        putIfPresent(fields, "durationMs", includeDuration ? requestLogContext.getDurationMs(request) : null);
        putIfPresent(fields, "ip", includeIp ? requestLogContext.getClientIp(request) : null);
        return fields;
    }

    /**
     * 現在のリクエストに紐づく共通ログフィールドを生成する。
     *
     * @param status HTTPステータスコード
     * @param includeUserId ユーザーIDを含める場合はtrue
     * @param includeIp クライアントIPを含める場合はtrue
     * @param includeDuration 処理時間を含める場合はtrue
     * @return リクエストログ用のフィールド。リクエストコンテキスト外の場合は空のフィールド
     */
    public LinkedHashMap<String, Object> currentRequestFields(
            Integer status,
            boolean includeUserId,
            boolean includeIp,
            boolean includeDuration
    ) {
        HttpServletRequest request = requestLogContext.getCurrentRequest();
        return request != null
                ? requestFields(request, status, includeUserId, includeIp, includeDuration)
                : new LinkedHashMap<>();
    }

    /**
     * メールアドレスのローカルパートをマスクする。
     *
     * @param email マスク対象のメールアドレス
     * @return マスク済みメールアドレス。不正な形式または空文字の場合はnull
     */
    public String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return null;
        }

        String[] parts = email.split("@", 2);
        String localPart = parts[0];
        String domainPart = parts[1];

        String maskedLocalPart;
        if (localPart.length() <= 1) {
            maskedLocalPart = "*";
        } else if (localPart.length() == 2) {
            maskedLocalPart = localPart.charAt(0) + "*";
        } else {
            // 3文字以上は先頭2文字だけ残し、個人情報の露出を抑える。
            maskedLocalPart = localPart.substring(0, 2) + "***";
        }

        return maskedLocalPart + "@" + domainPart;
    }

    /**
     * 現在有効なSpringプロファイル名を取得する。
     *
     * @return 有効プロファイルのカンマ区切り文字列。未指定の場合は {@code default}
     */
    public String getEnvironmentName() {
        String[] activeProfiles = environment.getActiveProfiles();
        return activeProfiles.length == 0 ? "default" : String.join(",", activeProfiles);
    }

    /**
     * 起動時に読み込まれたプロファイル名を取得する。
     *
     * @return 読み込み済みプロファイル名
     */
    public String getLoadedProfiles() {
        return getEnvironmentName();
    }

    /**
     * ログに付与するアプリケーションバージョンを取得する。
     *
     * @return アプリケーションバージョン
     */
    public String getVersion() {
        return version;
    }

    /**
     * 値がnullでない場合だけフィールドへ追加する。
     *
     * @param fields 追加先フィールド
     * @param key フィールド名
     * @param value フィールド値
     */
    public static void putIfPresent(Map<String, Object> fields, String key, Object value) {
        if (value != null) {
            fields.put(key, value);
        }
    }

    /**
     * 指定されたチャンネルとレベルで構造化ログを出力する。
     *
     * @param channel ログチャンネル
     * @param level ログレベル
     * @param eventId イベントID
     * @param message ログメッセージ
     * @param fields 追加フィールド
     */
    private void log(LogChannel channel, Level level, String eventId, String message, Map<String, Object> fields) {
        String payload = StructuredLogJsonFormatter.format(
                level.name(),
                SERVICE_NAME,
                getEnvironmentName(),
                version,
                eventId,
                message,
                fields
        );

        // チャンネルごとのLoggerへ、指定されたレベルで整形済みJSONを流す。
        switch (level) {
            case ERROR -> channel.getLogger().error(payload);
            case WARN -> channel.getLogger().warn(payload);
            case INFO -> channel.getLogger().info(payload);
            case DEBUG -> channel.getLogger().debug(payload);
            default -> channel.getLogger().info(payload);
        }
    }
}
