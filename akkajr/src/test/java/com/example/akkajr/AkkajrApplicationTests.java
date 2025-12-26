package com.example.akkajr;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = AkkajrApplication.class)
@ActiveProfiles("test")
@org.junit.jupiter.api.Disabled("Désactivé : test de contexte non nécessaire ici")
class AkkajrApplicationTests {

	@Test
	void contextLoads() {
	}

}
