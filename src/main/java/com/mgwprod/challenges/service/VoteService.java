package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.VoteRepository;
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

    @Transactional(readOnly = true)
    public boolean alreadyVoted(Long submissionId, Long voterId) {
        return voteRepository.existsBySubmissionIdAndVoterId(submissionId, voterId);
    }

    @Transactional(readOnly = true)
    public boolean isOwnSubmission(Long submissionId, Long voterId) {
        Submission submission = submissionService.getById(submissionId);
        return submission != null && submission.getProducerId().equals(voterId);
    }

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
