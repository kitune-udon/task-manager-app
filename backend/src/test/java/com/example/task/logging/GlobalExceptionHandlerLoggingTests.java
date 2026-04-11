package com.example.task.logging;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.example.task.exception.GlobalExceptionHandler;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.info.BuildProperties;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.mock.web.MockHttpServletRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerLoggingTests {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RequestLogContext requestLogContext = new RequestLogContext();

    private Logger applicationLogger;
    private ListAppender<ILoggingEvent> applicationAppender;

    @BeforeEach
    void setUp() {
        applicationLogger = (Logger) LoggerFactory.getLogger("application");
        applicationAppender = new ListAppender<>();
        applicationAppender.setName("application-handler-test");
        applicationAppender.start();
        applicationLogger.addAppender(applicationAppender);
    }

    @AfterEach
    void tearDown() {
        applicationLogger.detachAppender(applicationAppender);
        applicationAppender.stop();
    }

    @Test
    void includesStackTraceWhenEnabled() {
        StructuredLogService structuredLogService = new StructuredLogService(
                new MockEnvironment(),
                requestLogContext,
                buildPropertiesProvider()
        );
        LoggingProperties loggingProperties = new LoggingProperties();
        loggingProperties.setIncludeStacktrace(true);

        GlobalExceptionHandler handler = new GlobalExceptionHandler(
                structuredLogService,
                requestLogContext,
                loggingProperties
        );

        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/test/logging/runtime-error");
        request.setAttribute("requestId", "req-test-001");
        request.setAttribute(RequestLogContext.REQUEST_START_NANOS_ATTRIBUTE, System.nanoTime());

        handler.handleUnexpected(new IllegalStateException("boom"), request);

        JsonNode payload = firstPayload();
        assertThat(payload.path("eventId").asText()).isEqualTo("LOG-SYS-001");
        assertThat(payload.has("stackTrace")).isTrue();
        assertThat(payload.path("stackTrace").asText()).contains("IllegalStateException: boom");
    }

    private JsonNode firstPayload() {
        try {
            return objectMapper.readTree(applicationAppender.list.get(0).getFormattedMessage());
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private ObjectProvider<BuildProperties> buildPropertiesProvider() {
        DefaultListableBeanFactory beanFactory = new DefaultListableBeanFactory();
        Properties properties = new Properties();
        properties.setProperty("build.version", "0.0.1-SNAPSHOT");
        beanFactory.registerSingleton("buildProperties", new BuildProperties(properties));
        return beanFactory.getBeanProvider(BuildProperties.class);
    }
}
