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

// Lógica de negocio de las propuestas de colaboración: quién puede aceptarlas/
// rechazarlas o borrarlas, y el registro de cuándo se decidió cada una.
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

    // El controller la usa para devolver 403 antes de decidir.
    // Solo el productor dueño del beat original puede aceptar/rechazar la propuesta —
    // por eso hay que ir del topline hasta su beat para encontrar quién es el productor.
    @Transactional(readOnly = true)
    public boolean canDecide(Collaboration collaboration, Long requestingUserId) {
        Topline topline = toplineService.getById(collaboration.getToplineId());
        return topline != null && topline.getBeat().getProducerId().equals(requestingUserId);
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

    // El controller la usa para devolver 403 antes de borrar. A diferencia de
    // canDecide, acá puede borrar tanto el artista que subió el topline como el
    // productor dueño del beat (o un admin) — cualquiera de las dos partes de la
    // colaboración puede darla de baja.
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
