package com.mgwprod.challenges.model;

// No es una tabla: se arma al calcular el ranking.
public record RankingEntry(Long producerId, int totalPoints) {
}
