package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.repository.ChallengeRepository;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lógica de negocio de los challenges: quién puede crearlos, CRUD básico, y el
// "opportunity pick" que elige el artista invitado.
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

    // El controller la usa para devolver 403 antes de crear. Solo ADMIN o DISCOGRAFICA
    // pueden lanzar un challenge — un artista participa pero no organiza el concurso.
    @Transactional(readOnly = true)
    public boolean canCreateChallenge(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && (user.getRole() == Role.ADMIN || user.getRole() == Role.DISCOGRAFICA);
    }

    @Transactional(readOnly = true)
    public boolean userExists(Long userId) {
        return userRepository.findById(userId).isPresent();
    }

    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    @Transactional
    public Challenge create(Long requestingUserId, Challenge challenge) {
        challenge.setCreatedBy(requestingUserId);
        return challengeRepository.save(challenge);
    }

    @Transactional(readOnly = true)
    public List<Challenge> list() {
        return challengeRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Challenge getById(Long id) {
        return challengeRepository.findById(id).orElse(null);
    }

    // El controller la usa para devolver 403 antes de elegir el opportunity pick.
    @Transactional(readOnly = true)
    public boolean isGuestArtist(Challenge challenge, Long requestingUserId) {
        return challenge.getGuestArtistId().equals(requestingUserId);
    }

    // Devuelve null si la submission no pertenece a este challenge — evita que el
    // artista invitado elija como "pick" una submission de otro concurso.
    @Transactional
    public Challenge setOpportunityPick(Challenge challenge, Long submissionId) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null || !submission.getChallenge().getId().equals(challenge.getId())) {
            return null;
        }
        challenge.setOpportunityPickSubmissionId(submissionId);
        return challengeRepository.save(challenge);
    }

    // El controller las usa para devolver 403 antes de editar/borrar.
    @Transactional(readOnly = true)
    public boolean canModify(Challenge challenge, Long requestingUserId) {
        if (challenge.getCreatedBy().equals(requestingUserId)) {
            return true;
        }
        User requester = userRepository.findById(requestingUserId).orElse(null);
        return requester != null && requester.getRole() == Role.ADMIN;
    }

    // No hay un campo "closed" en Challenge: se considera cerrado si ya tiene
    // resultados calculados (ver ChallengeResultService.close).
    @Transactional(readOnly = true)
    public boolean isClosed(Long challengeId) {
        return challengeResultRepository.existsByChallengeId(challengeId);
    }

    @Transactional
    public Challenge update(Long id, Challenge request) {
        Challenge challenge = getById(id);
        if (challenge == null) {
            return null;
        }
        if (request.getTitle() != null) challenge.setTitle(request.getTitle());
        if (request.getTheme() != null) challenge.setTheme(request.getTheme());
        if (request.getDeadline() != null) challenge.setDeadline(request.getDeadline());
        return challengeRepository.save(challenge);
    }

    @Transactional
    public void delete(Long id) {
        challengeRepository.deleteById(id);
    }
}
