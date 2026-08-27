package com.mgwprod.users.dto;

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
    private final boolean isAdmin;
    private final Instant createdAt;
    private final ProducerProfileDto producerProfile;
    private final ArtistProfileDto artistProfile;
}
