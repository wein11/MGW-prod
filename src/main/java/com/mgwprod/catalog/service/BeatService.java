package com.mgwprod.catalog.service;

import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
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

    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    // No es readOnly porque getOrCreate puede crear la suscripción.
    @Transactional
    public boolean isAtProductionLimit(Long producerId) {
        return subscriptionService.isAtProductionLimit(producerId);
    }

    @Transactional
    public Beat create(Long producerId, Beat beat) {
        subscriptionService.recordProduction(producerId);
        beat.setProducerId(producerId);
        return beatRepository.save(beat);
    }

    // producerId tiene prioridad; si no, se combinan genre y bpm.
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
        return beatRepository.findById(id).orElse(null);
    }

    @Transactional
    public Beat update(Long id, Beat request) {
        Beat beat = getById(id);
        if (beat == null) {
            return null;
        }
        if (request.getTitle() != null) beat.setTitle(request.getTitle());
        if (request.getGenre() != null) beat.setGenre(request.getGenre());
        if (request.getBpm() != null) beat.setBpm(request.getBpm());
        if (request.getKey() != null) beat.setKey(request.getKey());
        if (request.getAudioUrl() != null) beat.setAudioUrl(request.getAudioUrl());
        return beatRepository.save(beat);
    }

    @Transactional
    public void delete(Long id) {
        beatRepository.deleteById(id);
    }

    // Puede modificar el dueño del beat o un admin.
    @Transactional(readOnly = true)
    public boolean canModify(Beat beat, Long requestingUserId) {
        if (beat.getProducerId().equals(requestingUserId)) {
            return true;
        }
        User requester = userRepository.findById(requestingUserId).orElse(null);
        return requester != null && requester.getRole() == Role.ADMIN;
    }
}
