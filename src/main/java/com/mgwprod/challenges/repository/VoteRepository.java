package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    List<Vote> findBySubmissionId(Long submissionId);
    boolean existsBySubmissionIdAndVoterId(Long submissionId, Long voterId);
}
