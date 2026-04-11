package com.example.task;

import com.example.task.logging.StartupFailureLoggingListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * タスク管理アプリケーションの Spring Boot 起動クラス。
 */
@SpringBootApplication
public class TaskApplication {

	/**
	 * Spring コンテナを起動し、Web API を受け付けられる状態にする。
	 */
	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(TaskApplication.class);
		application.addListeners(new StartupFailureLoggingListener());
		application.run(args);
	}

}
