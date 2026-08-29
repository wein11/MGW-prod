package com.mgwprod.users.service;

import com.mgwprod.users.dto.ArtistProfileDto;
import com.mgwprod.users.dto.ProducerProfileDto;
import com.mgwprod.users.dto.UserResponse;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.ProducerProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.ProducerProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;

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

    public UserResponse getById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
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
