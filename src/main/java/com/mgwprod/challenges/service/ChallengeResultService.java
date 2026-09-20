package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.RankingEntry;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

// Se encarga de cerrar un challenge: calcula el puntaje de cada submission, arma el
// top 3, guarda los ChallengeResult y verifica automáticamente al ganador. También
// expone las consultas de resultados y el ranking general acumulado.
@Service
public class ChallengeResultService {

    // Puntos que se llevan el 1°, 2° y 3° puesto respectivamente.
    private static final int[] POINTS_BY_RANK = {500, 300, 150};
    private static final int TOP_N = 3;

    private final ChallengeResultRepository challengeResultRepository;
    private final SubmissionRepository submissionRepository;
    private final VoteRepository voteRepository;
    private final ChallengeScoringService challengeScoringService;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public ChallengeResultService(ChallengeResultRepository challengeResultRepository,
                                   SubmissionRepository submissionRepository,
                                   VoteRepository voteRepository,
                                   ChallengeScoringService challengeScoringService,
                                   UserRepository userRepository,
                                   ArtistProfileRepository artistProfileRepository) {
        this.challengeResultRepository = challengeResultRepository;
        this.submissionRepository = submissionRepository;
        this.voteRepository = voteRepository;
        this.challengeScoringService = challengeScoringService;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    // El controller la usa para devolver 403 antes de cerrar un challenge.
    @Transactional(readOnly = true)
    public boolean isAdmin(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ADMIN;
    }

    // El corazón del módulo challenges: cierra el concurso y arma el podio.
    @Transactional
    public List<ChallengeResult> close(Challenge challenge) {
        List<Submission> submissions = submissionRepository.findByChallengeId(challenge.getId());

        // Se arma una sola vez el set de productores verificados, porque
        // ChallengeScoringService lo necesita para cada submission (ver scoreFor) y así
        // se evita repetir la misma consulta una y otra vez más abajo.
        Set<Long> verifiedProducerIds = new HashSet<>();
        for (ArtistProfile profile : artistProfileRepository.findByVerifiedTrue()) {
            verifiedProducerIds.add(profile.getUser().getId());
        }

        // Calcula el puntaje de cada submission una sola vez y lo guarda en un mapa
        // (por id), para no tener que recalcularlo cada vez que se compara.
        Map<Long, Double> scoreBySubmissionId = new HashMap<>();
        for (Submission submission : submissions) {
            scoreBySubmissionId.put(submission.getId(), scoreFor(challenge, verifiedProducerIds, submission));
        }

        // Arma el podio a mano: selection sort simple. En cada vuelta busca, entre las
        // que quedan, la de mayor puntaje, y la deja en su posición final.
        List<Submission> ranked = new ArrayList<>(submissions);
        int podiumSize = Math.min(TOP_N, ranked.size());
        for (int i = 0; i < podiumSize; i++) {
            int bestIndex = i;
            for (int j = i + 1; j < ranked.size(); j++) {
                double scoreJ = scoreBySubmissionId.get(ranked.get(j).getId());
                double scoreBest = scoreBySubmissionId.get(ranked.get(bestIndex).getId());
                if (scoreJ > scoreBest) {
                    bestIndex = j;
                }
            }
            Submission temp = ranked.get(i);
            ranked.set(i, ranked.get(bestIndex));
            ranked.set(bestIndex, temp);
        }

        List<ChallengeResult> results = new ArrayList<>();
        for (int i = 0; i < podiumSize; i++) {
            Submission submission = ranked.get(i);
            int rank = i + 1;

            ChallengeResult result = new ChallengeResult();
            result.setChallengeId(challenge.getId());
            result.setSubmissionId(submission.getId());
            result.setRank(rank);
            result.setPointsAwarded(POINTS_BY_RANK[i]);
            result.setPrizeText(prizeFor(challenge, rank));
            if (rank == 1) {
                result.setBadge("Ganador del desafío");
                // Ganar un challenge verifica automáticamente al productor, sin que un
                // admin tenga que hacerlo a mano.
                verifyWinner(submission.getProducerId());
            }
            results.add(challengeResultRepository.save(result));
        }
        return results;
    }

    // Si viene producerId filtra solo los resultados de ese productor; si no, trae todos.
    @Transactional(readOnly = true)
    public List<ChallengeResult> listResults(Long producerId) {
        if (producerId != null) {
            List<Long> submissionIds = new ArrayList<>();
            for (Submission submission : submissionRepository.findByProducerId(producerId)) {
                submissionIds.add(submission.getId());
            }
            return challengeResultRepository.findBySubmissionIdIn(submissionIds);
        }
        return challengeResultRepository.findAll();
    }

    // Ranking general de "Music Score": suma los puntos de TODOS los challenges en los
    // que participó cada productor, no solo el último. orElseThrow() acá es seguro
    // porque un ChallengeResult nunca existe sin su Submission (se crean juntos en close).
    @Transactional(readOnly = true)
    public List<RankingEntry> ranking() {
        Map<Long, Integer> pointsByProducer = new HashMap<>();
        for (ChallengeResult result : challengeResultRepository.findAll()) {
            Submission submission = submissionRepository.findById(result.getSubmissionId()).orElseThrow();
            Long producerId = submission.getProducerId();
            if (pointsByProducer.containsKey(producerId)) {
                int current = pointsByProducer.get(producerId);
                pointsByProducer.put(producerId, current + result.getPointsAwarded());
            } else {
                pointsByProducer.put(producerId, result.getPointsAwarded());
            }
        }

        List<RankingEntry> entries = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : pointsByProducer.entrySet()) {
            entries.add(new RankingEntry(entry.getKey(), entry.getValue()));
        }

        // Ordena de mayor a menor puntaje a mano (mismo selection sort que en close()).
        for (int i = 0; i < entries.size(); i++) {
            int bestIndex = i;
            for (int j = i + 1; j < entries.size(); j++) {
                if (entries.get(j).totalPoints() > entries.get(bestIndex).totalPoints()) {
                    bestIndex = j;
                }
            }
            RankingEntry temp = entries.get(i);
            entries.set(i, entries.get(bestIndex));
            entries.set(bestIndex, temp);
        }
        return entries;
    }

    // Junta todos los votos de una submission y delega el cálculo ponderado en
    // ChallengeScoringService (ver esa clase para el detalle de los pesos).
    private double scoreFor(Challenge challenge, Set<Long> verifiedProducerIds, Submission submission) {
        List<Vote> votes = voteRepository.findBySubmissionId(submission.getId());
        return challengeScoringService.computeScore(challenge.getGuestArtistId(), verifiedProducerIds, votes);
    }

    // Cada puesto del podio tiene su propio premio, definido al crear el challenge.
    private String prizeFor(Challenge challenge, int rank) {
        if (rank == 1) {
            return challenge.getPrizeFirst();
        }
        if (rank == 2) {
            return challenge.getPrizeSecond();
        }
        if (rank == 3) {
            return challenge.getPrizeThird();
        }
        return null;
    }

    private void verifyWinner(Long producerId) {
        Optional<ArtistProfile> profileOpt = artistProfileRepository.findByUserId(producerId);
        if (profileOpt.isPresent()) {
            ArtistProfile profile = profileOpt.get();
            profile.setVerified(true);
            artistProfileRepository.save(profile);
        }
    }
}
