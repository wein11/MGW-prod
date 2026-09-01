package com.mgwprod.users.model;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;

class ProfileJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void producerProfileDoesNotSerializeOwningUser() throws Exception {
        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setDisplayName("Owner");
        owner.setRole(Role.PRODUCER);

        ProducerProfile profile = new ProducerProfile();
        profile.setUser(owner);
        profile.setGenres("RKT");

        String json = objectMapper.writeValueAsString(profile);

        assertFalse(json.contains("\"user\""));
    }

    @Test
    void artistProfileDoesNotSerializeOwningUser() throws Exception {
        User owner = new User();
        owner.setEmail("owner@test.com");
        owner.setDisplayName("Owner");
        owner.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setUser(owner);
        profile.setBio("bio");

        String json = objectMapper.writeValueAsString(profile);

        assertFalse(json.contains("\"user\""));
    }
}
