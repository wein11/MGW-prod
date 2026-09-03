package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VoteService {

    private final VoteRepository voteRepository;
    private final SubmissionService submissionService;

    public VoteService(VoteRepository voteRepository, SubmissionService submissionService) {
        this.voteRepository = voteRepository;
        this.submissionService = submissionService;
    }

    @Transactional
    public Vote create(Long submissionId, Long voterId, Vote vote) {
        submissionService.getById(submissionId); // valida que la submission exista
        if (voteRepository.existsBySubmissionIdAndVoterId(submissionId, voterId)) {
            throw new ForbiddenOperationException("Ya votaste esta submission");
        }
        vote.setSubmissionId(submissionId);
        vote.setVoterId(voterId);
        return voteRepository.save(vote);
    }
}
