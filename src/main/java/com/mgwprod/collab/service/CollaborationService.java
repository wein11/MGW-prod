package com.mgwprod.collab.service;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class CollaborationService {

    private final CollaborationRepository collaborationRepository;
    private final ToplineService toplineService;
    private final UserRepository userRepository;

    public CollaborationService(CollaborationRepository collaborationRepository,
                                 ToplineService toplineService,
                                 UserRepository userRepository) {
        this.collaborationRepository = collaborationRepository;
        this.toplineService = toplineService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public Collaboration getById(Long id) {
        return collaborationRepository.findById(id).orElse(null);
    }

    // Solo el productor dueño del beat puede aceptar o rechazar.
    @Transactional(readOnly = true)
    public boolean canDecide(Collaboration collaboration, Long requestingUserId) {
        Topline topline = toplineService.getById(collaboration.getToplineId());
        return topline != null && topline.getBeat().getProducerId().equals(requestingUserId);
    }

    public boolean isPending(Collaboration collaboration) {
        return collaboration.getStatus() == CollaborationStatus.PENDING;
    }

    @Transactional
    public Collaboration decide(Long collaborationId, CollaborationStatus decision) {
        Collaboration collaboration = getById(collaborationId);
        if (collaboration == null) {
            return null;
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

    @Transactional(readOnly = true)
    public boolean canDelete(Collaboration collaboration, Long requestingUserId) {
        Topline topline = toplineService.getById(collaboration.getToplineId());
        if (topline == null) {
            return false;
        }
        boolean isArtist = topline.getArtistId().equals(requestingUserId);
        boolean isProducer = topline.getBeat().getProducerId().equals(requestingUserId);
        if (isArtist || isProducer) {
            return true;
        }
        User requester = userRepository.findById(requestingUserId).orElse(null);
        return requester != null && requester.getRole() == Role.ADMIN;
    }

    @Transactional
    public void delete(Long id) {
        collaborationRepository.deleteById(id);
    }
}
