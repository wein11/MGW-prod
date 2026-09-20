package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    // Todos los votos que recibió una submission — se usa para calcular su puntaje total.
    List<Vote> findBySubmissionId(Long submissionId);
    // Combina dos condiciones con AND: sirve para chequear si un votante ya votó una
    // submission puntual, y así evitar que vote dos veces.
    boolean existsBySubmissionIdAndVoterId(Long submissionId, Long voterId);
}
