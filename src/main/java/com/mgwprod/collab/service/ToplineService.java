package com.mgwprod.collab.service;

import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.collab.repository.ToplineRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lógica de negocio de los toplines: crear (lo que dispara una Collaboration nueva),
// listar, editar y borrar, más los chequeos de permisos del controller.
@Service
public class ToplineService {

    private final ToplineRepository toplineRepository;
    private final CollaborationRepository collaborationRepository;
    private final UserRepository userRepository;
    private final BeatRepository beatRepository;
    private final SubscriptionService subscriptionService;

    public ToplineService(ToplineRepository toplineRepository,
                           CollaborationRepository collaborationRepository,
                           UserRepository userRepository,
                           BeatRepository beatRepository,
                           SubscriptionService subscriptionService) {
        this.toplineRepository = toplineRepository;
        this.collaborationRepository = collaborationRepository;
        this.userRepository = userRepository;
        this.beatRepository = beatRepository;
        this.subscriptionService = subscriptionService;
    }

    // El controller la usa para devolver 403 antes de crear.
    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    // El controller la usa para devolver 403 antes de crear.
    // No puede ser readOnly, por la misma razón que en BeatService: por dentro puede
    // terminar creando la suscripción del usuario si todavía no tenía una.
    @Transactional
    public boolean isAtProductionLimit(Long artistId) {
        return subscriptionService.isAtProductionLimit(artistId);
    }

    // Devuelve null si el beat referenciado no existe.
    // Al crear el topline también se crea automáticamente su Collaboration en PENDING
    // — el artista no la pide aparte, nace junto con el topline.
    @Transactional
    public Topline create(Long artistId, Topline topline) {
        Beat beat = beatRepository.findById(topline.getBeat().getId()).orElse(null);
        if (beat == null) {
            return null;
        }

        subscriptionService.recordProduction(artistId);

        topline.setBeat(beat);
        topline.setArtistId(artistId);
        Topline saved = toplineRepository.save(topline);

        Collaboration collaboration = new Collaboration();
        collaboration.setToplineId(saved.getId());
        collaboration.setStatus(CollaborationStatus.PENDING);
        collaborationRepository.save(collaboration);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<Topline> list(Long beatId, Long artistId) {
        if (beatId != null) {
            return toplineRepository.findByBeatId(beatId);
        }
        if (artistId != null) {
            return toplineRepository.findByArtistId(artistId);
        }
        return toplineRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Topline getById(Long id) {
        return toplineRepository.findById(id).orElse(null);
    }

    // El controller la usa para devolver 403 antes de editar/borrar.
    @Transactional(readOnly = true)
    public boolean canModify(Topline topline, Long requestingUserId) {
        if (topline.getArtistId().equals(requestingUserId)) {
            return true;
        }
        User requester = userRepository.findById(requestingUserId).orElse(null);
        return requester != null && requester.getRole() == Role.ADMIN;
    }

    @Transactional
    public Topline update(Long id, Topline request) {
        Topline topline = getById(id);
        if (topline == null) {
            return null;
        }
        if (request.getAudioUrl() != null) {
            topline.setAudioUrl(request.getAudioUrl());
        }
        return toplineRepository.save(topline);
    }

    @Transactional
    public void delete(Long id) {
        toplineRepository.deleteById(id);
    }
}
