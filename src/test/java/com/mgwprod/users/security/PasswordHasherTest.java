package com.mgwprod.users.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordHasherTest {

    private final PasswordHasher passwordHasher = new PasswordHasher();

    @Test
    void matchesReturnsTrueForCorrectPassword() {
        String hash = passwordHasher.hash("supersecret123");
        assertTrue(passwordHasher.matches("supersecret123", hash));
    }

    @Test
    void matchesReturnsFalseForWrongPassword() {
        String hash = passwordHasher.hash("supersecret123");
        assertFalse(passwordHasher.matches("wrongpassword", hash));
    }

    @Test
    void hashProducesDifferentOutputForSamePasswordDueToRandomSalt() {
        String hash1 = passwordHasher.hash("supersecret123");
        String hash2 = passwordHasher.hash("supersecret123");
        assertNotEquals(hash1, hash2);
    }
}
