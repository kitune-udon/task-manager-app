package com.example.task.service;

import com.example.task.dto.UserResponse;
import com.example.task.dto.auth.LoginRequest;
import com.example.task.dto.auth.LoginResponse;
import com.example.task.dto.auth.RegisterRequest;
import com.example.task.dto.auth.RegisterResponse;
import com.example.task.entity.User;
import com.example.task.exception.ConflictException;
import com.example.task.exception.ErrorCode;
import com.example.task.logging.RequestLogContext;
import com.example.task.logging.StructuredLogService;
import com.example.task.repository.UserRepository;
import com.example.task.security.CustomUserDetails;
import com.example.task.security.JwtUtil;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;

/**
 * ユーザー登録とログイン認証を担当するサービス。
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final StructuredLogService structuredLogService;
    private final RequestLogContext requestLogContext;

    /**
     * コンストラクタ。
     *
     * @param userRepository ユーザーリポジトリ
     * @param passwordEncoder パスワードエンコーダー
     * @param jwtUtil JWT ユーティリティ
     * @param structuredLogService 構造化ログサービス
     * @param requestLogContext リクエストログコンテキスト
     */
    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtUtil jwtUtil,
            StructuredLogService structuredLogService,
            RequestLogContext requestLogContext
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
        this.structuredLogService = structuredLogService;
        this.requestLogContext = requestLogContext;
    }

    /**
     * メールアドレス重複を防ぎつつ新規ユーザーを登録する。
     */
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            logRegisterFailure(HttpStatus.CONFLICT.value(), ErrorCode.USR_001.getCode(), ErrorCode.USR_001.getDefaultMessage(), request.getEmail());
            requestLogContext.suppressSystem4xxLog();
            throw new ConflictException(ErrorCode.USR_001);
        }

        // 保存前にパスワードをハッシュ化し、平文のまま保持しない。
        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        User saved = userRepository.save(user);
        logRegisterSuccess(saved);

        return RegisterResponse.builder()
                .id(saved.getId())
                .name(saved.getName())
                .email(saved.getEmail())
                .createdAt(saved.getCreatedAt())
                .build();
    }

    /**
     * メールアドレスとパスワードを照合し、成功時に JWT を返す。
     */
    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> buildBadCredentials(request.getEmail()));

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw buildBadCredentials(request.getEmail());
        }

        String token = jwtUtil.generateToken(CustomUserDetails.from(user));
        logLoginSuccess(user);

        return LoginResponse.builder()
                .token(token)
                .user(UserResponse.builder()
                        .id(user.getId())
                        .name(user.getName())
                        .email(user.getEmail())
                        .build())
                .build();
    }

    /**
     * 不正なメールアドレス又はパスワードの例外を生成してログを記録します。
     *
     * @param email メールアドレス
     * @return 不正認証例外
     */
    private BadCredentialsException buildBadCredentials(String email) {
        logLoginFailure(email);
        requestLogContext.suppressSystem4xxLog();
        return new BadCredentialsException("Invalid email or password.");
    }

    /**
     * ログイン成功をセキュリティログに記録します。
     *
     * @param user ログインしたユーザー
     */
    private void logLoginSuccess(User user) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(
                HttpStatus.OK.value(),
                false,
                false,
                false
        );
        fields.put("userId", user.getId());
        fields.put("email", structuredLogService.maskEmail(user.getEmail()));
        structuredLogService.infoSecurity("LOG-AUTH-001", "ログイン成功", fields);
    }

    /**
     * ログイン失敗をセキュリティログに記録します。
     *
     * @param email ログイン試行したメールアドレス
     */
    private void logLoginFailure(String email) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(
                HttpStatus.UNAUTHORIZED.value(),
                false,
                true,
                false
        );
        fields.put("errorCode", ErrorCode.AUTH_002.getCode());
        fields.put("email", structuredLogService.maskEmail(email));
        structuredLogService.warnSecurity("LOG-AUTH-002", "ログイン失敗", fields);
    }

    /**
     * ユーザー登録成功をセキュリティログに記録します。
     *
     * @param user 登録されたユーザー
     */
    private void logRegisterSuccess(User user) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(
                HttpStatus.CREATED.value(),
                false,
                false,
                false
        );
        fields.put("userId", user.getId());
        fields.put("email", structuredLogService.maskEmail(user.getEmail()));
        structuredLogService.infoSecurity("LOG-AUTH-004", "ユーザー登録成功", fields);
    }

    /**
     * ユーザー登録失敗をセキュリティログに記録します。
     *
     * @param status HTTP ステータスコード
     * @param errorCode エラーコード
     * @param safeMessage クライアントに返すメッセージ
     * @param email 登録試行したメールアドレス
     */
    private void logRegisterFailure(int status, String errorCode, String safeMessage, String email) {
        LinkedHashMap<String, Object> fields = structuredLogService.currentRequestFields(
                status,
                false,
                true,
                false
        );
        fields.put("errorCode", errorCode);
        fields.put("safeMessage", safeMessage);
        fields.put("email", structuredLogService.maskEmail(email));
        structuredLogService.warnSecurity("LOG-AUTH-005", "ユーザー登録失敗", fields);
    }
}
