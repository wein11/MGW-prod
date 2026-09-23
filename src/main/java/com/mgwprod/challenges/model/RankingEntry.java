package com.mgwprod.challenges.model;

// No es una entidad JPA (no tiene tabla propia): es solo el resultado calculado del
// ranking general, sumando los puntos de todos los ChallengeResult de cada productor.
// Un record alcanza porque es un dato inmutable de solo lectura, sin comportamiento.
public record RankingEntry(Long producerId, int totalPoints) {
}
