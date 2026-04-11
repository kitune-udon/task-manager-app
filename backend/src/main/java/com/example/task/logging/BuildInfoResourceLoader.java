package com.example.task.logging;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * build-info.properties からアプリバージョンを安全に読み出す補助クラス。
 */
public final class BuildInfoResourceLoader {

    private static final String BUILD_INFO_PATH = "META-INF/build-info.properties";
    private static final String VERSION_KEY = "build.version";

    private BuildInfoResourceLoader() {
    }

    public static String loadVersion() {
        try (InputStream inputStream = BuildInfoResourceLoader.class.getClassLoader().getResourceAsStream(BUILD_INFO_PATH)) {
            if (inputStream == null) {
                return "unknown";
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(VERSION_KEY, "unknown");
        } catch (IOException ex) {
            return "unknown";
        }
    }
}
