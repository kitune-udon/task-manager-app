package com.example.task.logging;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingSupportTests {

    private final RequestLogContext requestLogContext = new RequestLogContext();

    @Test
    void skipsHealthCheckAccessLogOn2xx() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(200);

        assertThat(requestLogContext.shouldSkipAccessLog(request, response)).isTrue();
    }

    @Test
    void keepsHealthCheckAccessLogOnNon2xx() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/actuator/health");
        MockHttpServletResponse response = new MockHttpServletResponse();
        response.setStatus(503);

        assertThat(requestLogContext.shouldSkipAccessLog(request, response)).isFalse();
    }

    @Test
    void resolvesClientIpFromForwardedFor() {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("X-Forwarded-For", "10.0.0.1, 10.0.0.2");

        assertThat(requestLogContext.getClientIp(request)).isEqualTo("10.0.0.1");
    }

    @Test
    void masksEmailBasedOnLocalPartLength() {
        StructuredLogService structuredLogService = new StructuredLogService(
                new MockEnvironment(),
                requestLogContext,
                buildPropertiesProvider()
        );

        assertThat(structuredLogService.maskEmail("a@example.com")).isEqualTo("*@example.com");
        assertThat(structuredLogService.maskEmail("ab@example.com")).isEqualTo("a*@example.com");
        assertThat(structuredLogService.maskEmail("abcd@example.com")).isEqualTo("ab***@example.com");
    }

    private org.springframework.beans.factory.ObjectProvider<BuildProperties> buildPropertiesProvider() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        Properties properties = new Properties();
        properties.setProperty("build.version", "0.0.1-SNAPSHOT");
        beanFactory.registerSingleton("buildProperties", new BuildProperties(properties));
        return beanFactory.getBeanProvider(BuildProperties.class);
    }
}
