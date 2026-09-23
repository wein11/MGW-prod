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

// Lógica de negocio de las entregas a un challenge: crear (mientras no pasó el
// deadline), listar y buscar.
@Service
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final UserRepository userRepository;
    // Se usa ChallengeRepository y no ChallengeService porque ChallengeService ya depende
    // de SubmissionService: si cada uno dependiera del otro, Spring no podría crearlos.
    private final ChallengeRepository challengeRepository;

    public SubmissionService(SubmissionRepository submissionRepository,
                              UserRepository userRepository,
                              ChallengeRepository challengeRepository) {
        this.submissionRepository = submissionRepository;
        this.userRepository = userRepository;
        this.challengeRepository = challengeRepository;
    }

    // El controller la usa para devolver 403 antes de crear.
    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    // Busca el challenge de la entrega; null si no existe.
    @Transactional(readOnly = true)
    public Challenge getChallenge(Long challengeId) {
        return challengeRepository.findById(challengeId).orElse(null);
    }

    // El controller la usa para devolver 403 si alguien intenta mandar una submission
    // después de la fecha límite del challenge.
    @Transactional(readOnly = true)
    public boolean isPastDeadline(Challenge challenge) {
        return Instant.now().isAfter(challenge.getDeadline());
    }

    // Devuelve null si el challenge no existe.
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

    // Todas las entregas de un challenge.
    @Transactional(readOnly = true)
    public List<Submission> listByChallenge(Long challengeId) {
        return submissionRepository.findByChallengeId(challengeId);
    }

    // Busca una entrega por id; null si no existe.
    @Transactional(readOnly = true)
    public Submission getById(Long id) {
        return submissionRepository.findById(id).orElse(null);
    }
}
