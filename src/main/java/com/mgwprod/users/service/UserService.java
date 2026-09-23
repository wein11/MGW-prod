package com.mgwprod.users.service;

import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
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
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isArtist(User user) {
        return user.getRole() == Role.ARTIST;
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    @Transactional(readOnly = true)
    public ArtistProfile getProfile(Long userId) {
        return artistProfileRepository.findByUserId(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isOwner(Long targetUserId, Long requestingUserId) {
        return targetUserId.equals(requestingUserId);
    }

    // Solo se actualizan los campos que vienen en el request.
    @Transactional
    public User updateUser(Long targetUserId, User request) {
        User user = getById(targetUserId);
        if (user == null) {
            return null;
        }
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        return userRepository.save(user);
    }

    @Transactional
    public ArtistProfile updateArtistProfile(Long targetUserId, ArtistProfile request) {
        ArtistProfile profile = artistProfileRepository.findByUserId(targetUserId).orElseThrow();
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
    public ArtistProfile verifyArtist(Long artistId) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artistId).orElseThrow();
        profile.setVerified(true);
        return artistProfileRepository.save(profile);
    }

    // flush() fuerza el DELETE acá para poder atrapar el error de FK y devolver false.
    @Transactional
    public boolean delete(Long targetUserId) {
        try {
            userRepository.deleteById(targetUserId);
            userRepository.flush();
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
