package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
class CollaborationRepositoryTest {

    @Autowired
    private CollaborationRepository collaborationRepository;

    @Autowired
    private ToplineRepository toplineRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByToplineIdReturnsTheCollaboration() {
        User artist = new User();
        artist.setEmail("artist-collab-repo-test@example.com");
        artist.setPasswordHash("hash");
        artist.setDisplayName("Artist");
        artist.setRole(Role.ARTIST);
        artist.setCreatedAt(Instant.now());
        User savedArtist = userRepository.save(artist);

        Topline topline = new Topline();
        topline.setArtistId(savedArtist.getId());
        topline.setBeatId(2L);
        topline.setAudioUrl("https://soundcloud.com/example/topline");
        Topline savedTopline = toplineRepository.save(topline);

        Collaboration collaboration = new Collaboration();
        collaboration.setToplineId(savedTopline.getId());
        collaboration.setStatus(CollaborationStatus.PENDING);
        collaborationRepository.save(collaboration);

        Optional<Collaboration> result = collaborationRepository.findByToplineId(savedTopline.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getStatus()).isEqualTo(CollaborationStatus.PENDING);
    }
}
