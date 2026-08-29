package com.mgwprod.users.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.mgwprod.users.model.Role;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.Instant;

@Getter
@AllArgsConstructor
public class UserResponse {
    private final Long id;
    private final String email;
    private final String displayName;
    private final Role role;
    private final String city;
    // Field name must match the Lombok boolean getter's implicit property name ("admin",
    // from isAdmin() -> "admin") so both merge into one Jackson property before being
    // renamed to "isAdmin" by @JsonProperty. Otherwise Jackson serializes it twice
    // (once as "admin" from the field, once as "isAdmin" from the annotation).
    @JsonProperty("isAdmin")
    private final boolean admin;
    private final Instant createdAt;
    private final ProducerProfileDto producerProfile;
    private final ArtistProfileDto artistProfile;
}
