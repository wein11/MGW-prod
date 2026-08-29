package com.mgwprod.users.service;

import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UserNotFoundException;
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
    void getByIdReturnsUserWithProducerProfile() {
        User user = new User();
        user.setId(1L);
        user.setEmail("productor@test.com");
        user.setDisplayName("DJ Test");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("RKT,Trap");
        profile.setBpmMin(90);
        profile.setBpmMax(140);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));

        UserResponse response = userService.getById(1L);

        assertEquals("productor@test.com", response.getEmail());
        assertEquals("RKT,Trap", response.getProducerProfile().getGenres());
    }

    @Test
    void getByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
    }
}
