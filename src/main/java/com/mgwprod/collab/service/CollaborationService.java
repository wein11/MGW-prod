package com.mgwprod.collab.service;

import com.mgwprod.catalog.exception.BeatNotFoundException;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.exception.CollaborationNotFoundException;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CollaborationService {

    private final CollaborationRepository collaborationRepository;
    private final ToplineService toplineService;
    private final BeatRepository beatRepository;

    public CollaborationService(CollaborationRepository collaborationRepository,
                                 ToplineService toplineService,
                                 BeatRepository beatRepository) {
        this.collaborationRepository = collaborationRepository;
        this.toplineService = toplineService;
        this.beatRepository = beatRepository;
    }

    @Transactional
    public Collaboration decide(Long collaborationId, Long requestingUserId, CollaborationStatus decision) {
        Collaboration collaboration = collaborationRepository.findById(collaborationId)
                .orElseThrow(() -> new CollaborationNotFoundException(collaborationId));

        Topline topline = toplineService.getById(collaboration.getToplineId());
        Beat beat = beatRepository.findById(topline.getBeatId())
                .orElseThrow(() -> new BeatNotFoundException(topline.getBeatId()));

        if (!beat.getProducerId().equals(requestingUserId)) {
            throw new ForbiddenOperationException("Solo el productor dueño del beat puede decidir esta colaboración");
        }

        collaboration.setStatus(decision);
        collaboration.setDecidedAt(Instant.now());
        return collaborationRepository.save(collaboration);
    }

    @Transactional(readOnly = true)
    public List<Collaboration> listByStatus(CollaborationStatus status) {
        if (status != null) {
            return collaborationRepository.findByStatus(status);
        }
        return collaborationRepository.findAll();
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Collaboration collaboration = collaborationRepository.findById(id)
                .orElseThrow(() -> new CollaborationNotFoundException(id));
        Topline topline = toplineService.getById(collaboration.getToplineId());
        Beat beat = beatRepository.findById(topline.getBeatId())
                .orElseThrow(() -> new BeatNotFoundException(topline.getBeatId()));

        boolean isArtist = topline.getArtistId().equals(requestingUserId);
        boolean isProducer = beat.getProducerId().equals(requestingUserId);
        if (!isArtist && !isProducer) {
            throw new ForbiddenOperationException("Solo las partes de esta colaboración pueden borrarla");
        }
        collaborationRepository.deleteById(id);
    }
}
