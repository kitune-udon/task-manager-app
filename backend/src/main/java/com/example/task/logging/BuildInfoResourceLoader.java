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

    /**
     * ユーティリティクラスのためインスタンス化を禁止する。
     */
    private BuildInfoResourceLoader() {
    }

    /**
     * クラスパス上の {@code build-info.properties} からアプリケーションバージョンを読み込む。
     *
     * @return 読み込んだバージョン。リソースやキーが存在しない場合、または読み込みに失敗した場合は {@code unknown}
     */
    public static String loadVersion() {
        try (InputStream inputStream = BuildInfoResourceLoader.class.getClassLoader().getResourceAsStream(BUILD_INFO_PATH)) {
            // build-info.properties はビルド方法によって存在しないことがあるため、ログ出力は継続できるようにする。
            if (inputStream == null) {
                return "unknown";
            }

            Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty(VERSION_KEY, "unknown");
        } catch (IOException ex) {
            // バージョン取得に失敗してもアプリケーション起動やログ出力は止めない。
            return "unknown";
        }
    }
}
