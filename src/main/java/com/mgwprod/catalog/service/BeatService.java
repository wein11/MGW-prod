package com.mgwprod.catalog.service;

import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.exception.BeatNotFoundException;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeatService {

    private final BeatRepository beatRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public BeatService(BeatRepository beatRepository, UserRepository userRepository,
                        SubscriptionService subscriptionService) {
        this.beatRepository = beatRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    @Transactional
    public Beat create(Long producerId, Beat beat) {
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo un artista puede publicar beats");
        }
        subscriptionService.recordProduction(producerId);
        beat.setProducerId(producerId);
        return beatRepository.save(beat);
    }

    @Transactional(readOnly = true)
    public List<Beat> list(String genre, Integer bpm, Long producerId) {
        if (producerId != null) {
            return beatRepository.findByProducerId(producerId);
        }
        if (genre != null && bpm != null) {
            return beatRepository.findByGenreAndBpm(genre, bpm);
        }
        if (genre != null) {
            return beatRepository.findByGenre(genre);
        }
        if (bpm != null) {
            return beatRepository.findByBpm(bpm);
        }
        return beatRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Beat getById(Long id) {
        return beatRepository.findById(id)
                .orElseThrow(() -> new BeatNotFoundException(id));
    }

    @Transactional
    public Beat update(Long id, Long requestingUserId, Beat request) {
        Beat beat = getById(id);
        requireOwnerOrAdmin(beat.getProducerId(), requestingUserId);

        if (request.getTitle() != null) beat.setTitle(request.getTitle());
        if (request.getGenre() != null) beat.setGenre(request.getGenre());
        if (request.getBpm() != null) beat.setBpm(request.getBpm());
        if (request.getKey() != null) beat.setKey(request.getKey());
        if (request.getAudioUrl() != null) beat.setAudioUrl(request.getAudioUrl());
        return beatRepository.save(beat);
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Beat beat = getById(id);
        requireOwnerOrAdmin(beat.getProducerId(), requestingUserId);
        beatRepository.deleteById(id);
    }

    private void requireOwnerOrAdmin(Long ownerId, Long requestingUserId) {
        if (ownerId.equals(requestingUserId)) {
            return;
        }
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo el dueño del beat o un admin pueden hacer esto");
        }
    }
}
