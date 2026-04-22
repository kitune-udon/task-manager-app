package com.example.task;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 5. 担当者候補一覧のAPI観点を検証する。
 */
class UserCandidatesApiIntegrationTests extends ApiIntegrationTestBase {

    @Test
    @DisplayName("USR-01: 認証済みユーザーは /api/users で候補一覧を取得できる")
    void getUsersReturnsCandidates() throws Exception {
        createUser("Bravo User", "bravo@example.com", "password123");
        createUser("Alpha User", "alpha@example.com", "password123");

        String token = loginAndGetToken("alpha@example.com", "password123");

        mockMvc.perform(get("/api/users")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].name").value("Alpha User"))
                .andExpect(jsonPath("$[1].name").value("Bravo User"))
                .andExpect(jsonPath("$[0].email").value("alpha@example.com"));
    }

    @Test
    @DisplayName("USR-04: 未認証では担当者候補一覧を取得できない")
    void usersEndpointRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ERR-AUTH-001"));
    }
}
