package com.mgwprod.challenges.service;

import com.mgwprod.challenges.exception.ChallengeNotFoundException;
import com.mgwprod.challenges.exception.SubmissionNotFoundException;
import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    // Se depende de ChallengeRepository (no de ChallengeService) para evitar el ciclo de
    // beans: desde Task 8 ChallengeService depende de SubmissionService (opportunity-pick).
    private final ChallengeRepository challengeRepository;

    public SubmissionService(SubmissionRepository submissionRepository,
                              UserRepository userRepository,
                              ChallengeRepository challengeRepository) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
    }

    @Transactional
    public Submission create(Long challengeId, Long producerId, Submission submission) {
        User producer = userRepository.findById(producerId)
                .orElseThrow(() -> new UserNotFoundException(producerId));
        if (producer.getRole() != Role.PRODUCER) {
            throw new ForbiddenOperationException("Solo un productor puede enviar una submission");
        }
        Challenge challenge = challengeRepository.findById(challengeId)
                .orElseThrow(() -> new ChallengeNotFoundException(challengeId));
        if (Instant.now().isAfter(challenge.getDeadline())) {
            throw new ForbiddenOperationException("El deadline de este challenge ya pasó");
        }
        submission.setChallengeId(challengeId);
        submission.setProducerId(producerId);
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<Submission> listByChallenge(Long challengeId) {
        return submissionRepository.findByChallengeId(challengeId);
    }

    @Transactional(readOnly = true)
    public Submission getById(Long id) {
        return submissionRepository.findById(id)
                .orElseThrow(() -> new SubmissionNotFoundException(id));
    }
}
