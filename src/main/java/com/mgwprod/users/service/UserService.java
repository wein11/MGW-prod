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
    public ProducerProfile updateProducerProfile(Long targetUserId, Long requestingUserId, ProducerProfile request) {
        User user = requireOwnership(targetUserId, requestingUserId);
        if (user.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Este usuario no tiene perfil de productor");
        }

        ProducerProfile profile = producerProfileRepository.findByUserId(targetUserId)
                .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + targetUserId));
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
        return producerProfileRepository.save(profile);
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
        return artistProfileRepository.save(profile);
    }

    @Transactional
    public ProducerProfile verifyProducer(Long requestingUserId, Long producerId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (!requester.isAdmin()) {
            throw new ForbiddenOperationException("Solo un admin puede verificar productores");
        }
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Solo se puede verificar a un productor");
        }
        ProducerProfile profile = producerProfileRepository.findByUserId(producerId)
                .orElseThrow(() -> new IllegalStateException("Producer sin perfil: " + producerId));
        profile.setVerified(true);
        return producerProfileRepository.save(profile);
    }

    private User requireOwnership(Long targetUserId, Long requestingUserId) {
        if (!targetUserId.equals(requestingUserId)) {
            throw new ForbiddenOperationException("No podés editar el perfil de otro usuario");
        }
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new UserNotFoundException(targetUserId));
    }
}
