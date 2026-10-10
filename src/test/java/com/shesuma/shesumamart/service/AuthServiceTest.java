package com.shesuma.shesumamart.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AuthServiceTest {
    @Test
    void passwordShouldHashAndVerify() {
        AuthService service = new AuthService();
        String hash = service.hashPassword("secret123");

        assertNotEquals("secret123", hash);
        assertTrue(service.verifyPassword("secret123", hash));
        assertFalse(service.verifyPassword("wrong", hash));
    }
}
