package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Challenge;
import org.springframework.data.jpa.repository.JpaRepository;

// No necesita ningún método propio: los CRUD básicos de JpaRepository (save,
// findById, findAll, deleteById) alcanzan para todo lo que usa ChallengeService.
public interface ChallengeRepository extends JpaRepository<Challenge, Long> {
}
