package com.mgwprod.users.service;

import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public UserService(UserRepository userRepository, ArtistProfileRepository artistProfileRepository) {
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public ArtistProfile getProfile(Long userId) {
        User user = getById(userId);
        if (user.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Este usuario no tiene perfil de artista");
        }
        return artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + userId));
    }

    @Transactional
    public User updateUser(Long targetUserId, Long requestingUserId, User request) {
        User user = requireOwnership(targetUserId, requestingUserId);

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        return userRepository.save(user);
    }

    @Transactional
    public ArtistProfile updateArtistProfile(Long targetUserId, Long requestingUserId, ArtistProfile request) {
        User user = requireOwnership(targetUserId, requestingUserId);
        if (user.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Este usuario no tiene perfil de artista");
        }

        ArtistProfile profile = artistProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + targetUserId));
        if (request.getGenres() != null) {
            profile.setGenres(request.getGenres());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getBpmMin() != null) {
            profile.setBpmMin(request.getBpmMin());
        }
        if (request.getBpmMax() != null) {
            profile.setBpmMax(request.getBpmMax());
        }
        if (request.getExperienceLevel() != null) {
            profile.setExperienceLevel(request.getExperienceLevel());
        }
        return artistProfileRepository.save(profile);
    }

    @Transactional
    public ArtistProfile verifyArtist(Long requestingUserId, Long artistId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo un admin puede verificar artistas");
        }
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new UserNotFoundException(artistId));
        if (artist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo se puede verificar a un artista");
        }
        ArtistProfile profile = artistProfileRepository.findByUserId(artistId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + artistId));
        profile.setVerified(true);
        return artistProfileRepository.save(profile);
    }

    private User requireOwnership(Long targetUserId, Long requestingUserId) {
        if (!targetUserId.equals(requestingUserId)) {
            throw new ForbiddenOperationException("No podés editar el perfil de otro usuario");
        }
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
    }
}
