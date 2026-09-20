package com.mgwprod.users.service;

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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
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
    void getByIdReturnsNullWhenUserDoesNotExist() {
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThat(userService.getById(99L)).isNull();
    }

    @Test
    void isArtistIsTrueForArtistRole() {
        User user = new User();
        user.setRole(Role.ARTIST);

        assertThat(userService.isArtist(user)).isTrue();
    }

    @Test
    void isArtistIsFalseForOtherRoles() {
        User user = new User();
        user.setRole(Role.DISCOGRAFICA);

        assertThat(userService.isArtist(user)).isFalse();
    }

    @Test
    void isAdminIsTrueForAdminRole() {
        User user = new User();
        user.setRole(Role.ADMIN);

        assertThat(userService.isAdmin(user)).isTrue();
    }

    @Test
    void getProfileReturnsArtistProfile() {
        ArtistProfile profile = new ArtistProfile();
        profile.setBio("bio");
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));

        ArtistProfile response = userService.getProfile(2L);

        assertEquals(profile, response);
    }

    @Test
    void isOwnerIsTrueWhenIdsMatch() {
        assertThat(userService.isOwner(1L, 1L)).isTrue();
        assertThat(userService.isOwner(1L, 2L)).isFalse();
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

        User response = userService.updateUser(1L, request);

        assertEquals("New Name", response.getDisplayName());
    }

    @Test
    void updateUserReturnsNullWhenMissing() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        User request = new User();
        request.setDisplayName("New Name");

        assertThat(userService.updateUser(1L, request)).isNull();
    }

    @Test
    void updateArtistProfileChangesGenresAndBioForOwner() {
        ArtistProfile profile = new ArtistProfile();
        profile.setGenres("Old");
        profile.setBio("Old bio");

        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile request = new ArtistProfile();
        request.setGenres("RKT,Trap");
        request.setBio("New bio");

        ArtistProfile response = userService.updateArtistProfile(1L, request);

        assertEquals("RKT,Trap", response.getGenres());
        assertEquals("New bio", response.getBio());
    }

    @Test
    void updateArtistProfileChangesProducerFieldsForOwner() {
        ArtistProfile profile = new ArtistProfile();

        when(artistProfileRepository.findByUserId(1L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(any(ArtistProfile.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ArtistProfile request = new ArtistProfile();
        request.setBpmMin(120);
        request.setBpmMax(140);
        request.setExperienceLevel("intermedio");

        ArtistProfile response = userService.updateArtistProfile(1L, request);

        assertEquals(120, response.getBpmMin());
        assertEquals(140, response.getBpmMax());
        assertEquals("intermedio", response.getExperienceLevel());
    }

    @Test
    void verifyArtistSetsVerifiedTrue() {
        ArtistProfile profile = new ArtistProfile();
        profile.setVerified(false);
        when(artistProfileRepository.findByUserId(2L)).thenReturn(Optional.of(profile));
        when(artistProfileRepository.save(profile)).thenReturn(profile);

        ArtistProfile result = userService.verifyArtist(2L);

        assertThat(result.isVerified()).isTrue();
    }

    @Test
    void deleteRemovesUserWithNoContent() {
        boolean deleted = userService.delete(1L);

        assertThat(deleted).isTrue();
        verify(userRepository).deleteById(1L);
    }

    @Test
    void deleteReturnsFalseWhenUserHasContent() {
        doThrow(new org.springframework.dao.DataIntegrityViolationException("FK violation"))
                .when(userRepository).deleteById(1L);

        assertThat(userService.delete(1L)).isFalse();
    }
}
