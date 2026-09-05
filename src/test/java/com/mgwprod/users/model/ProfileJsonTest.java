package com.mgwprod.users.model;

import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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

    @Test
    void artistProfileSerializesMergedProducerFields() throws Exception {
        ArtistProfile profile = new ArtistProfile();
        profile.setGenres("RKT");
        profile.setBpmMin(120);
        profile.setBpmMax(140);
        profile.setExperienceLevel("intermedio");
        profile.setVerified(true);

        String json = objectMapper.writeValueAsString(profile);

        assertTrue(json.contains("\"bpmMin\":120"));
        assertTrue(json.contains("\"bpmMax\":140"));
        assertTrue(json.contains("\"experienceLevel\":\"intermedio\""));
        assertTrue(json.contains("\"verified\":true"));
    }
}
