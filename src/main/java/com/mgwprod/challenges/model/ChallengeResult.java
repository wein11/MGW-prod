package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// El resultado final de una submission dentro de un challenge ya cerrado: en qué
// puesto quedó, cuántos puntos sumó y qué premio le corresponde. Se genera una fila
// por submission al momento de cerrar el challenge (ver ChallengeResultService.close)
// y a partir de ahí el challenge se considera cerrado (ChallengeService.isClosed
// chequea si existe algún resultado para ese challengeId).
@Entity
@Table(name = "challenge_results")
@Getter
@Setter
@NoArgsConstructor
public class ChallengeResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "challenge_id", nullable = false)
    private Long challengeId;

    // unique = true: cada submission tiene como mucho un resultado.
    @Column(name = "submission_id", nullable = false, unique = true)
    private Long submissionId;

    @Column(name = "rank_position", nullable = false)
    private Integer rank;

    @Column(name = "points_awarded", nullable = false)
    private Integer pointsAwarded;

    private String badge;

    @Column(name = "prize_text")
    private String prizeText;
}
