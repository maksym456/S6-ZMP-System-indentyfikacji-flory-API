package com.ezielnik.api;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "jwt.secret=test-secret-key-for-unit-tests-only-must-be-at-least-32-characters-long"
})
class ApiApplicationTests {

    @Test
    void contextLoads() {
    }

}
