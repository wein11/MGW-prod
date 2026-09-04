package com.mgwprod.challenges.service;

import com.mgwprod.challenges.exception.ChallengeNotFoundException;
import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
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

    public ChallengeService(ChallengeRepository challengeRepository,
                             UserRepository userRepository,
                             SubmissionService submissionService) {
        this.challengeRepository = challengeRepository;
        this.userRepository = userRepository;
        this.submissionService = submissionService;
    }

    @Transactional
    public Challenge create(Long requestingUserId, Challenge challenge) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (!requester.isAdmin()) {
            throw new ForbiddenOperationException("Solo un admin puede crear challenges");
        }
        User guestArtist = userRepository.findById(challenge.getGuestArtistId())
                .orElseThrow(() -> new UserNotFoundException(challenge.getGuestArtistId()));
        if (guestArtist.getRole() != Role.ARTIST) {
            throw new ForbiddenOperationException("El artista invitado debe tener rol ARTIST");
        }
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
}
