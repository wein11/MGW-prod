package com.mgwprod.collab.service;

import com.mgwprod.catalog.exception.BeatNotFoundException;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.collab.exception.ToplineNotFoundException;
import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CollaborationRepository;
import com.mgwprod.collab.repository.ToplineRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ToplineService {

    private final ToplineRepository toplineRepository;
    private final CollaborationRepository collaborationRepository;
    private final UserRepository userRepository;
    private final BeatRepository beatRepository;

    public ToplineService(ToplineRepository toplineRepository,
                           CollaborationRepository collaborationRepository,
                           UserRepository userRepository,
                           BeatRepository beatRepository) {
        this.toplineRepository = toplineRepository;
        this.collaborationRepository = collaborationRepository;
        this.userRepository = userRepository;
        this.beatRepository = beatRepository;
    }

    @Transactional
    public Topline create(Long artistId, Topline topline) {
        User artist = userRepository.findById(artistId)
                .orElseThrow(() -> new UserNotFoundException(artistId));
        if (artist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("Solo un artista puede subir un topline");
        }
        beatRepository.findById(topline.getBeatId())
                .orElseThrow(() -> new BeatNotFoundException(topline.getBeatId()));

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
        return toplineRepository.findById(id)
                .orElseThrow(() -> new ToplineNotFoundException(id));
    }
}
