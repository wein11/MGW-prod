package com.mgwprod.challenges.repository;

import com.mgwprod.challenges.model.Challenge;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
class ChallengeRepositoryTest {

    @Autowired
    private ChallengeRepository challengeRepository;

    @Test
    void savesAndReturnsChallenge() {
        Challenge challenge = new Challenge();
        challenge.setTitle("Creamos el próximo hit de RKT");
        challenge.setGenre("RKT");
        challenge.setBpm(100);
        challenge.setTheme("libre");
        challenge.setDeadline(Instant.now().plus(7, ChronoUnit.DAYS));
        challenge.setGuestArtistId(1L);

        Challenge saved = challengeRepository.save(challenge);

        assertThat(saved.getId()).isNotNull();
        assertThat(challengeRepository.findById(saved.getId())).isPresent();
    }
}
