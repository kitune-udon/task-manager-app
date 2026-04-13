package com.example.task;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Spring コンテナが最低限起動できることを確認するスモークテスト。
 */
@SpringBootTest
@ActiveProfiles("test")
class TaskApplicationTests {

	@Test
	void contextLoads() {
	}

}
