package ru.mssecondteam.taskservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class TaskServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
