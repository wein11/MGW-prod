package com.mgwprod.challenges.service;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.model.ChallengeResult;
import com.mgwprod.challenges.model.Submission;
import com.mgwprod.challenges.model.Vote;
import com.mgwprod.challenges.repository.ChallengeResultRepository;
import com.mgwprod.challenges.repository.SubmissionRepository;
import com.mgwprod.challenges.repository.VoteRepository;
import com.mgwprod.users.exception.ForbiddenOperationException;
import com.mgwprod.users.exception.UserNotFoundException;
import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ChallengeResultService {

    private static final int[] POINTS_BY_RANK = {500, 300, 150};
    private static final int TOP_N = 3;

    private final ChallengeResultRepository challengeResultRepository;
    private final SubmissionRepository submissionRepository;
    private final VoteRepository voteRepository;
    private final ChallengeService challengeService;
    private final ChallengeScoringService challengeScoringService;
    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public ChallengeResultService(ChallengeResultRepository challengeResultRepository,
                                   SubmissionRepository submissionRepository,
                                   VoteRepository voteRepository,
                                   ChallengeService challengeService,
                                   ChallengeScoringService challengeScoringService,
                                   UserRepository userRepository,
                                   ArtistProfileRepository artistProfileRepository) {
        this.challengeResultRepository = challengeResultRepository;
        this.submissionRepository = submissionRepository;
        this.voteRepository = voteRepository;
        this.challengeService = challengeService;
        this.challengeScoringService = challengeScoringService;
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    @Transactional
    public List<ChallengeResult> close(Long challengeId, Long requestingUserId) {
        User requester = userRepository.findById(requestingUserId)
                .orElseThrow(() -> new UserNotFoundException(requestingUserId));
        if (requester.getRole() != Role.ADMIN) {
            throw new ForbiddenOperationException("Solo un admin puede cerrar un challenge");
        }

        Challenge challenge = challengeService.getById(challengeId);
        List<Submission> submissions = submissionRepository.findByChallengeId(challengeId);

        Set<Long> verifiedProducerIds = artistProfileRepository.findByVerifiedTrue().stream()
                .map(ArtistProfile::getUser)
                .map(User::getId)
                .collect(Collectors.toSet());

        List<Submission> ranked = submissions.stream()
                .sorted(Comparator.comparingDouble(
                        (Submission submission) -> scoreFor(challenge, verifiedProducerIds, submission)
                ).reversed())
                .limit(TOP_N)
                .toList();

        List<ChallengeResult> results = new java.util.ArrayList<>();
        for (int i = 0; i < ranked.size(); i++) {
            Submission submission = ranked.get(i);
            int rank = i + 1;

            ChallengeResult result = new ChallengeResult();
            result.setChallengeId(challengeId);
            result.setSubmissionId(submission.getId());
            result.setRank(rank);
            result.setPointsAwarded(POINTS_BY_RANK[i]);
            result.setPrizeText(prizeFor(challenge, rank));
            if (rank == 1) {
                result.setBadge("Ganador del desafío");
                verifyWinner(submission.getProducerId());
            }
            results.add(challengeResultRepository.save(result));
        }
        return results;
    }

    @Transactional(readOnly = true)
    public List<ChallengeResult> listResults(Long producerId) {
        if (producerId != null) {
            List<Long> submissionIds = submissionRepository.findByProducerId(producerId).stream()
                    .map(Submission::getId)
                    .toList();
            return challengeResultRepository.findBySubmissionIdIn(submissionIds);
        }
        return challengeResultRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<com.mgwprod.challenges.model.RankingEntry> ranking() {
        java.util.Map<Long, Integer> pointsByProducer = new java.util.HashMap<>();
        for (ChallengeResult result : challengeResultRepository.findAll()) {
            Submission submission = submissionRepository.findById(result.getSubmissionId())
                    .orElseThrow(() -> new com.mgwprod.challenges.exception.SubmissionNotFoundException(result.getSubmissionId()));
            pointsByProducer.merge(submission.getProducerId(), result.getPointsAwarded(), Integer::sum);
        }
        return pointsByProducer.entrySet().stream()
                .map(entry -> new com.mgwprod.challenges.model.RankingEntry(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparingInt(com.mgwprod.challenges.model.RankingEntry::totalPoints).reversed())
                .toList();
    }

    private double scoreFor(Challenge challenge, Set<Long> verifiedProducerIds, Submission submission) {
        List<Vote> votes = voteRepository.findBySubmissionId(submission.getId());
        return challengeScoringService.computeScore(challenge.getGuestArtistId(), verifiedProducerIds, votes);
    }

    private String prizeFor(Challenge challenge, int rank) {
        return switch (rank) {
            case 1 -> challenge.getPrizeFirst();
            case 2 -> challenge.getPrizeSecond();
            case 3 -> challenge.getPrizeThird();
            default -> null;
        };
    }

    private void verifyWinner(Long producerId) {
        artistProfileRepository.findByUserId(producerId).ifPresent(profile -> {
            profile.setVerified(true);
            artistProfileRepository.save(profile);
        });
    }
}
