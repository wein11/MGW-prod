package com.mgwprod.users.service;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
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
    void updateUserChangesDisplayNameForOwner() {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("Old Name");
        user.setRole(Role.PRODUCER);
        user.setCreatedAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User request = new User();
        request.setDisplayName("New Name");

        User response = userService.updateUser(1L, 1L, request);

        assertEquals("New Name", response.getDisplayName());
    }

    @Test
    void updateUserThrowsForbiddenWhenEditingSomeoneElse() {
        User request = new User();
        request.setDisplayName("New Name");

        assertThrows(ForbiddenOperationException.class, () -> userService.updateUser(1L, 2L, request));
    }

    @Test
    void updateProducerProfileChangesGenresForOwner() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.PRODUCER);

        ProducerProfile profile = new ProducerProfile();
        profile.setGenres("Old");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(producerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(producerProfileRepository.save(any(ProducerProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProducerProfile request = new ProducerProfile();
        request.setGenres("RKT,Trap");

        ProducerProfile response = userService.updateProducerProfile(1L, 1L, request);

        assertEquals("RKT,Trap", response.getGenres());
    }

    @Test
    void updateProducerProfileThrowsForbiddenWhenUserIsNotProducer() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ARTIST);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ProducerProfile request = new ProducerProfile();
        request.setGenres("RKT");

        assertThrows(ForbiddenOperationException.class,
                () -> userService.updateProducerProfile(1L, 1L, request));
    }

    @Test
    void updateArtistProfileChangesBioForOwner() {
        User user = new User();
        user.setId(2L);
        user.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setBio("Old bio");

        when(userRepository.findById(2L)).thenReturn(Optional.of(user));
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile request = new ArtistProfile();
        request.setBio("New bio");

        ArtistProfile response = userService.updateArtistProfile(2L, 2L, request);

        assertEquals("New bio", response.getBio());
    }

    @Test
    void verifyProducerSetsVerifiedTrueWhenRequesterIsAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setAdmin(true);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User producer = new User();
        producer.setId(2L);
        producer.setRole(Role.PRODUCER);
        when(userRepository.findById(2L)).thenReturn(Optional.of(producer));

        ProducerProfile profile = new ProducerProfile();
        profile.setVerified(false);
        when(producerProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(producerProfileRepository.save(profile)).thenReturn(profile);

        ProducerProfile result = userService.verifyProducer(1L, 2L);

        assertThat(result.isVerified()).isTrue();
    }

    @Test
    void verifyProducerThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setAdmin(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        assertThatThrownBy(() -> userService.verifyProducer(1L, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
