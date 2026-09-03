package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.ChallengeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeResultRepository extends JpaRepository<ChallengeResult, Long> {
    List<ChallengeResult> findByChallengeId(Long challengeId);
    List<ChallengeResult> findBySubmissionIdIn(List<Long> submissionIds);
}
