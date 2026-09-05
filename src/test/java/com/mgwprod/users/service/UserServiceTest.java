package com.mgwprod.users.service;

import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
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
    private ArtistProfileRepository artistProfileRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        userService = new UserService(userRepository, artistProfileRepository);
    }

    @Test
    void getByIdReturnsUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("artista@test.com");
        user.setDisplayName("DJ Test");
        user.setRole(Role.ARTIST);
        user.setCreatedAt(Instant.now());

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        User response = userService.getById(1L);

        assertEquals("artista@test.com", response.getEmail());
    }

    @Test
    void getByIdThrowsWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(UserNotFoundException.class, () -> userService.getById(99L));
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

        ArtistProfile response = userService.getProfile(2L);

        assertEquals(profile, response);
    }

    @Test
    void getProfileThrowsForbiddenWhenUserIsNotArtist() {
        User user = new User();
        user.setId(3L);
        user.setRole(Role.DISCOGRAFICA);

        when(userRepository.findById(3L)).thenReturn(Optional.of(user));

        assertThrows(ForbiddenOperationException.class, () -> userService.getProfile(3L));
    }

    @Test
    void updateUserChangesDisplayNameForOwner() {
        User user = new User();
        user.setId(1L);
        user.setDisplayName("Old Name");
        user.setRole(Role.ARTIST);
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
    void updateArtistProfileChangesGenresAndBioForOwner() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();
        profile.setGenres("Old");
        profile.setBio("Old bio");

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile request = new ArtistProfile();
        request.setGenres("RKT,Trap");
        request.setBio("New bio");

        ArtistProfile response = userService.updateArtistProfile(1L, 1L, request);

        assertEquals("RKT,Trap", response.getGenres());
        assertEquals("New bio", response.getBio());
    }

    @Test
    void updateArtistProfileChangesProducerFieldsForOwner() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.ARTIST);

        ArtistProfile profile = new ArtistProfile();

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));
        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile request = new ArtistProfile();
        request.setBpmMin(120);
        request.setBpmMax(140);
        request.setExperienceLevel("intermedio");

        ArtistProfile response = userService.updateArtistProfile(1L, 1L, request);

        assertEquals(120, response.getBpmMin());
        assertEquals(140, response.getBpmMax());
        assertEquals("intermedio", response.getExperienceLevel());
    }

    @Test
    void updateArtistProfileThrowsForbiddenWhenUserIsNotArtist() {
        User user = new User();
        user.setId(1L);
        user.setRole(Role.DISCOGRAFICA);

        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        ArtistProfile request = new ArtistProfile();
        request.setGenres("RKT");

        assertThrows(ForbiddenOperationException.class,
                () -> userService.updateArtistProfile(1L, 1L, request));
    }

    @Test
    void verifyArtistSetsVerifiedTrueWhenRequesterIsAdmin() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User artist = new User();
        artist.setId(2L);
        artist.setRole(Role.ARTIST);
        when(userRepository.findById(2L)).thenReturn(Optional.of(artist));

        ArtistProfile profile = new ArtistProfile();
        profile.setVerified(false);
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(profile)).thenReturn(profile);

        ArtistProfile result = userService.verifyArtist(1L, 2L);

        assertThat(result.isVerified()).isTrue();
    }

    @Test
    void verifyArtistThrowsWhenRequesterIsNotAdmin() {
        User notAdmin = new User();
        notAdmin.setId(1L);
        notAdmin.setRole(Role.ARTIST);
        when(userRepository.findById(1L)).thenReturn(Optional.of(notAdmin));

        assertThatThrownBy(() -> userService.verifyArtist(1L, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }

    @Test
    void verifyArtistThrowsWhenTargetIsNotArtist() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);
        when(userRepository.findById(1L)).thenReturn(Optional.of(admin));

        User discografica = new User();
        discografica.setId(2L);
        discografica.setRole(Role.DISCOGRAFICA);
        when(userRepository.findById(2L)).thenReturn(Optional.of(discografica));

        assertThatThrownBy(() -> userService.verifyArtist(1L, 2L))
                .isInstanceOf(ForbiddenOperationException.class);
    }
}
