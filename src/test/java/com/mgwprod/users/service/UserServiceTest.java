package com.mgwprod.users.service;

import com.mgwprod.users.dto.UpdateUserRequest;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

class UserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private ProducerProfileRepository producerProfileRepository;
    @Mock
    private ArtistProfileRepository artistProfileRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, producerProfileRepository, artistProfileRepository);
    }

    @Test
    void getByIdReturnsUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setDisplayName("DJ Test");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User response = userService.getById(1L);

        assertEquals("productor@test.com", response.getEmail());
    }

    @Test
    void getByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
    }

    @Test
    void getProfileReturnsProducerProfileForProducer() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.PRODUCER);

        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("RKT,Trap");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        Object response = userService.getProfile(1L);

        assertEquals(profile, response);
    }

    @Test
    void getProfileReturnsArtistProfileForArtist() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setBio("bio");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

        Object response = userService.getProfile(2L);

        assertEquals(profile, response);
    }

    @Test
    void updateChangesDisplayNameForOwner() {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("Old Name");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        ProducerProfile profile = new ProducerProfile();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("New Name");

        UserResponse response = userService.update(1L, 1L, request);

        assertEquals("New Name", response.getDisplayName());
    }

    @Test
    void updateThrowsForbiddenWhenEditingSomeoneElse() {
        UpdateUserRequest request = new UpdateUserRequest();
        request.setDisplayName("New Name");

        assertThrows(ForbiddenOperationException.class, () -> userService.update(1L, 2L, request));
    }
}
