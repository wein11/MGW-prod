package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
}
