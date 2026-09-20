package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Vote;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

// Calcula el puntaje final de una submission a partir de sus votos, con jurado
// ponderado: no todos los votos valen lo mismo, según quién vota.
@Service
public class ChallengeScoringService {

    // Los tres pesos suman 1.0 (100%): comunidad, productores verificados y el
    // artista invitado, cada uno aporta su parte al puntaje final.
    private static final double COMMUNITY_WEIGHT = 0.30;
    private static final double VERIFIED_WEIGHT = 0.30;
    private static final double GUEST_WEIGHT = 0.40;

    public double computeScore(Long guestArtistId, Set<Long> verifiedProducerIds, List<Vote> votes) {
        List<Integer> communityScores = new ArrayList<>();
        List<Integer> verifiedScores = new ArrayList<>();
        Integer guestScore = null;

        // Clasifica cada voto en uno de los tres grupos, según quién lo emitió.
        for (Vote vote : votes) {
            if (vote.getVoterId().equals(guestArtistId)) {
                guestScore = vote.getScore();
            } else if (verifiedProducerIds.contains(vote.getVoterId())) {
                verifiedScores.add(vote.getScore());
            } else {
                communityScores.add(vote.getScore());
            }
        }

        double communityAvg = average(communityScores);
        double verifiedAvg = average(verifiedScores);
        // Si el artista invitado no votó todavía, su parte del puntaje es 0 en vez de
        // romper el cálculo — así se puede ordenar el ranking incluso antes de que vote.
        double guestComponent = guestScore != null ? guestScore : 0.0;

        return COMMUNITY_WEIGHT * communityAvg + VERIFIED_WEIGHT * verifiedAvg + GUEST_WEIGHT * guestComponent;
    }

    // Promedio simple de una lista de puntajes; 0 si no hay ningún voto de ese grupo
    // (para no dividir por cero).
    private double average(List<Integer> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }
        int total = 0;
        for (Integer score : scores) {
            total += score;
        }
        return (double) total / scores.size();
    }
}
