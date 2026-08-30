package com.mgwprod.users.service;

import com.mgwprod.users.dto.ArtistProfileDto;
import com.mgwprod.users.dto.ProducerProfileDto;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final ProducerProfileRepository producerProfileRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public UserService(UserRepository userRepository,
                        ProducerProfileRepository producerProfileRepository,
                        ArtistProfileRepository artistProfileRepository) {
        this.userRepository = userRepository;
        this.producerProfileRepository = producerProfileRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional(readOnly = true)
    public Object getProfile(Long userId) {
        User user = getById(userId);
        if (user.getRole() == Role.PRODUCER) {
            return producerProfileRepository.findByUserId(userId)
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + userId));
        }
        return artistProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + userId));
    }

    @Transactional
    public UserResponse update(Long targetUserId, Long requestingUserId, UpdateUserRequest request) {
        if (!targetUserId.equals(requestingUserId)) {
            throw new ForbiddenOperationException("No podés editar el perfil de otro usuario");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));

        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        userRepository.save(user);

        if (user.getRole() == Role.PRODUCER) {
            ProducerProfile profile = producerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + user.getId()));
            if (request.getGenres() != null) {
                profile.setGenres(request.getGenres());
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
            producerProfileRepository.save(profile);
        } else {
            ArtistProfile profile = artistProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + user.getId()));
            if (request.getGenres() != null) {
                profile.setGenres(request.getGenres());
            }
            if (request.getBio() != null) {
                profile.setBio(request.getBio());
            }
            artistProfileRepository.save(profile);
        }

        return toResponse(user);
    }

    private UserResponse toResponse(User user) {
        ProducerProfileDto producerDto = null;
        ArtistProfileDto artistDto = null;

        if (user.getRole() == Role.PRODUCER) {
            ProducerProfile profile = producerProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + user.getId()));
            producerDto = new ProducerProfileDto(profile.getGenres(), profile.getBpmMin(),
                    profile.getBpmMax(), profile.getExperienceLevel());
        } else {
            ArtistProfile profile = artistProfileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalStateException("Artist sin perfil: " + user.getId()));
            artistDto = new ArtistProfileDto(profile.getGenres(), profile.getBio());
        }

        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(),
                user.getRole(), user.getCity(), user.isAdmin(), user.getCreatedAt(),
                producerDto, artistDto);
    }
}
