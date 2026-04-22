package com.example.task;

import com.example.task.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.util.Map;

import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 3. 認証・セッションのAPI観点を検証する。
 */
class AuthSessionApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("REG-01: 新規登録の正常系でユーザーを作成できる")
    void registerSucceeds() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Test User",
                                "email", "register@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isCreated())
                .andExpect(header().exists("X-Request-Id"))
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Test User"))
                .andExpect(jsonPath("$.email").value("register@example.com"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("REG-03: 新規登録のバリデーションエラー時に項目エラーを返す")
    void registerValidationFails() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "",
                                "email", "invalid-mail",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-INPUT-001"))
                .andExpect(jsonPath("$.details", hasSize(3)))
                .andExpect(jsonPath("$.details[*].field", hasItems("name", "email", "password")));
    }

    @Test
    @DisplayName("LGN-01: ログイン成功時に token と user を返し、認証確認 API が利用できる")
    void loginAndAuthMeSucceed() throws Exception {
        User user = createUser("Login User", "login@example.com", "password123");

        String token = loginAndGetToken("login@example.com", "password123");

        mockMvc.perform(get("/api/auth-test/me")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.email").value("login@example.com"));
    }

    @Test
    @DisplayName("LGN-05: ログイン認証失敗時は ERR-AUTH-002 を返す")
    void loginFailsWithBadCredentials() throws Exception {
        createUser("Login User", "login@example.com", "password123");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "email", "login@example.com",
                                "password", "wrongpass123"
                        ))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-002"));
    }

    @Test
    @DisplayName("SES-01: token なしの保護 API は ERR-AUTH-001 を返す")
    void protectedApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-001"));
    }

    @Test
    @DisplayName("SES-02: 不正 token の保護 API は ERR-AUTH-003 を返す")
    void invalidTokenReturnsAuth003() throws Exception {
        mockMvc.perform(get("/api/tasks")
                        .header("Authorization", bearer("invalid-token")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-003"));
    }

    @Test
    @DisplayName("LGN-04: ログイン入力不正時は項目エラーを返す")
    void loginValidationFails() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "email", "invalid-mail",
                                "password", "short"
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ERR-INPUT-001"))
                .andExpect(jsonPath("$.details[*].field", hasItems("email", "password")));
    }

    @Test
    @DisplayName("REG-05: 登録済みメールアドレスでは新規登録できない")
    void registerRejectsDuplicateEmail() throws Exception {
        createUser("Registered User", "registered@example.com", "password123");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(asJson(Map.of(
                                "name", "Duplicate User",
                                "email", "registered@example.com",
                                "password", "password123"
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ERR-USR-001"));
    }
}
