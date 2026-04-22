package com.example.task.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.context.ConfigurationPropertiesAutoConfiguration;
import org.springframework.boot.autoconfigure.validation.ValidationAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class JwtPropertiesValidationTests {

    private static final String TEST_SECRET = "QUJDREVGR0hJSktMTU5PUFFSU1RVVldYWVo0MTIzNDU2Nzg5MDEyMzQ=";

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    ConfigurationPropertiesAutoConfiguration.class,
                    ValidationAutoConfiguration.class
            ))
            .withUserConfiguration(TestConfiguration.class);

    @Test
    @DisplayName("JWT secret 未設定では設定バインドに失敗する")
    void failsWhenSecretIsMissing() {
        contextRunner
                .withPropertyValues("app.jwt.expiration-millis=3600000")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure()).hasStackTraceContaining("field 'secret'");
                });
    }

    @Test
    @DisplayName("JWT secret 設定時は設定値をバインドできる")
    void bindsWhenSecretIsPresent() {
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=" + TEST_SECRET,
                        "app.jwt.expiration-millis=3600000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).getSecret()).isEqualTo(TEST_SECRET);
                });
    }

    @Test
    @DisplayName("JWT secret の前後空白は除去して利用できる")
    void trimsSecretBeforeValidation() {
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=  " + TEST_SECRET + "  ",
                        "app.jwt.expiration-millis=3600000"
                )
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(JwtProperties.class).getSecret()).isEqualTo(TEST_SECRET);
                });
    }

    @Test
    @DisplayName("JWT secret が Base64 不正なら起動時に失敗する")
    void failsWhenSecretIsNotBase64() {
        contextRunner
                .withPropertyValues(
                        "app.jwt.secret=not-base64-secret",
                        "app.jwt.expiration-millis=3600000"
                )
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasStackTraceContaining("app.jwt.secret must be a valid Base64 string");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(JwtProperties.class)
    static class TestConfiguration {
    }
}
