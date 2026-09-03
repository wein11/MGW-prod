package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByChallengeId(Long challengeId);
    List<Submission> findByProducerId(Long producerId);
}
