package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
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
    // Se usa el repository y no ChallengeService para no tener dependencia circular.
    private final ChallengeRepository challengeRepository;

    public SubmissionService(SubmissionRepository submissionRepository,
                              UserRepository userRepository,
                              ChallengeRepository challengeRepository) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
    }

    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    @Transactional(readOnly = true)
    public Challenge getChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isPastDeadline(Challenge challenge) {
        return Instant.now().isAfter(challenge.getDeadline());
    }

    @Transactional
    public Submission create(Long challengeId, Long producerId, Submission submission) {
        Challenge challenge = getChallenge(challengeId);
        if (challenge == null) {
            return null;
        }
        submission.setChallenge(challenge);
        submission.setProducerId(producerId);
        return submissionRepository.save(submission);
    }

    @Transactional(readOnly = true)
    public List<Submission> listByChallenge(Long challengeId) {
        return submissionRepository.findByChallengeId(challengeId);
    }

    @Transactional(readOnly = true)
    public Submission getById(Long id) {
        return submissionRepository.findById(id).orElse(null);
    }
}
