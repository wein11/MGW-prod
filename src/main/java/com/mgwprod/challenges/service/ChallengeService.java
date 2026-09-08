package com.mgwprod.challenges.service;

import com.mgwprod.challenges.exception.ChallengeNotFoundException;
import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChallengeService {

    private final ChallengeRepository challengeRepository;
    private final UserRepository userRepository;
    private final SubmissionService submissionService;
    private final ChallengeResultRepository challengeResultRepository;

    public ChallengeService(ChallengeRepository challengeRepository,
                             UserRepository userRepository,
                             SubmissionService submissionService,
                             ChallengeResultRepository challengeResultRepository) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
        this.challengeResultRepository = challengeResultRepository;
    }

    @Transactional
    public Challenge create(Long requestingUserId, Challenge challenge) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN && requester.getRole() != Role.DISCOGRAFICA) {
            throw new ForbiddenOperationException("Solo un admin o una discográfica pueden crear challenges");
        }
        User guestArtist = userRepository.findById(challenge.getGuestArtistId())
                .orElseThrow(() -> new UserNotFoundException(challenge.getGuestArtistId()));
        if (guestArtist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("El artista invitado debe tener rol ARTIST");
        }
        challenge.setCreatedBy(requestingUserId);
        return challengeRepository.save(challenge);
    }

    @Transactional(readOnly = true)
    public List<Challenge> list() {
        return challengeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Challenge getById(Long id) {
        return challengeRepository.findById(id)
                .orElseThrow(() -> new ChallengeNotFoundException(id));
    }

    @Transactional
    public Challenge setOpportunityPick(Long challengeId, Long requestingUserId, Long submissionId) {
        Challenge challenge = getById(challengeId);
        if (!challenge.getGuestArtistId().equals(requestingUserId)) {
            throw new ForbiddenOperationException("Solo el artista invitado de este challenge puede elegir su opportunity pick");
        }
        Submission submission = submissionService.getById(submissionId);
        if (!submission.getChallengeId().equals(challengeId)) {
            throw new ForbiddenOperationException("La submission no pertenece a este challenge");
        }
        challenge.setOpportunityPickSubmissionId(submissionId);
        return challengeRepository.save(challenge);
    }

    @Transactional
    public Challenge update(Long id, Long requestingUserId, Challenge request) {
        Challenge challenge = getById(id);
        requireOwnerOrAdmin(challenge.getCreatedBy(), requestingUserId);
        if (challengeResultRepository.existsByChallengeId(id)) {
            throw new ForbiddenOperationException("No se puede editar un challenge ya cerrado");
        }
        if (request.getTitle() != null) challenge.setTitle(request.getTitle());
        if (request.getTheme() != null) challenge.setTheme(request.getTheme());
        if (request.getDeadline() != null) challenge.setDeadline(request.getDeadline());
        return challengeRepository.save(challenge);
    }

    @Transactional
    public void delete(Long id, Long requestingUserId) {
        Challenge challenge = getById(id);
        requireOwnerOrAdmin(challenge.getCreatedBy(), requestingUserId);
        challengeRepository.deleteById(id);
    }

    private void requireOwnerOrAdmin(Long ownerId, Long requestingUserId) {
        if (ownerId.equals(requestingUserId)) {
            return;
        }
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo quien creó el challenge o un admin pueden hacer esto");
        }
    }
}
