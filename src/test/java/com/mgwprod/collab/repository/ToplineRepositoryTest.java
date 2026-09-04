package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Topline;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class ToplineRepositoryTest {

    @Autowired
    private ToplineRepository toplineRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByBeatIdReturnsOnlyToplinesForThatBeat() {
        User artist = new User();
        artist.setEmail("artist-topline-repo-test@example.com");
        artist.setPasswordHash("hash");
        artist.setDisplayName("Artist");
        artist.setRole(Role.ARTIST);
        artist.setCreatedAt(Instant.now());
        User savedArtist = userRepository.save(artist);

        Topline topline = new Topline();
        topline.setArtistId(savedArtist.getId());
        topline.setBeatId(2L);
        topline.setAudioUrl("https://soundcloud.com/example/topline");
        toplineRepository.save(topline);

        List<Topline> result = toplineRepository.findByBeatId(2L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getArtistId()).isEqualTo(savedArtist.getId());
    }
}
