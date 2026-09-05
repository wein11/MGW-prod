package com.mgwprod.users.model;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void passwordHashIsNeverSerialized() throws Exception {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPasswordHash("some-hash");
        user.setDisplayName("Test");
        user.setRole(Role.ARTIST);

        String json = objectMapper.writeValueAsString(user);

        assertFalse(json.contains("passwordHash"));
        assertFalse(json.contains("some-hash"));
    }

    @Test
    void plaintextPasswordIsAcceptedOnInputButNeverSerialized() throws Exception {
        String incomingJson = """
                {"email":"test@test.com","password":"supersecret123","displayName":"Test","role":"ARTIST"}
                """;

        User user = objectMapper.readValue(incomingJson, User.class);
        assertEquals("supersecret123", user.getPassword());

        String outgoingJson = objectMapper.writeValueAsString(user);
        assertFalse(outgoingJson.contains("password\""));
    }

    @Test
    void roleAcceptsAdminAsAnyOtherEnumValue() throws Exception {
        User user = new User();
        user.setEmail("test@test.com");
        user.setDisplayName("Test");
        user.setRole(Role.ADMIN);

        String json = objectMapper.writeValueAsString(user);

        assertTrue(json.contains("\"role\":\"ADMIN\""));
        assertFalse(json.contains("\"isAdmin\""));
        assertFalse(json.contains("\"admin\":"));
    }
}
