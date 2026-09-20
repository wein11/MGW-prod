package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Submission;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    // Aunque el campo se llame `challenge` (objeto Challenge), Spring Data resuelve
    // findByChallengeId como WHERE challenge_id = ? sobre esa relación.
    List<Submission> findByChallengeId(Long challengeId);
    List<Submission> findByProducerId(Long producerId);
}
