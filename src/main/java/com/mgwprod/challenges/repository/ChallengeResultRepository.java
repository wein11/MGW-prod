package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.ChallengeResult;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChallengeResultRepository extends JpaRepository<ChallengeResult, Long> {
    // Trae la tabla de posiciones completa de un challenge ya cerrado.
    List<ChallengeResult> findByChallengeId(Long challengeId);
    // "In": trae los resultados de varias submissions a la vez (una sola consulta con
    // WHERE submission_id IN (...) en vez de una consulta por cada id).
    List<ChallengeResult> findBySubmissionIdIn(List<Long> submissionIds);
    // Se usa como el chequeo de "¿este challenge ya está cerrado?" — si existe algún
    // resultado para el challenge, es porque ya se cerró.
    boolean existsByChallengeId(Long challengeId);
}
