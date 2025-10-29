package com.ns.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "jwt.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA", 

    "google.client.id=dummy-google-id",
    "google.client.secret=dummy-google-secret",
    "google.client.redirect-uri=http://test.local/auth/google/callback",
    
    "yorkie.jwt.secret=BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB",
    "yorkie.webhook.secret=dummy-webhook-secret",
    
    "frontend.redirect.url=http://test.local/main"
})
class UserApplicationTests {

	@Test
	void contextLoads() {
	}

}
