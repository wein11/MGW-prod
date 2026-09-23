package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.VoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Lógica de negocio de los votos: chequear si alguien ya votó y crear un voto nuevo.
@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final SubmissionService submissionService;

    public VoteService(VoteRepository voteRepository, SubmissionService submissionService) {
        this.voteRepository = voteRepository;
        this.submissionService = submissionService;
    }

    // El controller la usa para devolver 403 antes de crear — un mismo voter no puede
    // votar dos veces la misma submission.
    @Transactional(readOnly = true)
    public boolean alreadyVoted(Long submissionId, Long voterId) {
        return voteRepository.existsBySubmissionIdAndVoterId(submissionId, voterId);
    }

    // Devuelve null si la submission no existe.
    @Transactional
    public Vote create(Long submissionId, Long voterId, Vote vote) {
        Submission submission = submissionService.getById(submissionId);
        if (submission == null) {
            return null;
        }
        vote.setSubmissionId(submissionId);
        vote.setVoterId(voterId);
        return voteRepository.save(vote);
    }
}
