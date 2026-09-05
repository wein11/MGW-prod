package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Comment;
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
class CommentRepositoryTest {

    @Autowired
    private CommentRepository commentRepository;

    @Autowired
    private ToplineRepository toplineRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByToplineIdReturnsOnlyThatToplinesComments() {
        User artist = new User();
        artist.setEmail("artist-comment-repo-test@example.com");
        artist.setPasswordHash("hash");
        artist.setDisplayName("Artist");
        artist.setRole(Role.ARTIST);
        artist.setCreatedAt(Instant.now());
        User savedArtist = userRepository.save(artist);

        User commenter = new User();
        commenter.setEmail("commenter-comment-repo-test@example.com");
        commenter.setPasswordHash("hash");
        commenter.setDisplayName("Commenter");
        commenter.setRole(Role.ARTIST);
        commenter.setCreatedAt(Instant.now());
        User savedCommenter = userRepository.save(commenter);

        Topline topline = new Topline();
        topline.setArtistId(savedArtist.getId());
        topline.setBeatId(2L);
        topline.setAudioUrl("https://soundcloud.com/example/topline");
        Topline savedTopline = toplineRepository.save(topline);

        Comment comment = new Comment();
        comment.setToplineId(savedTopline.getId());
        comment.setAuthorId(savedCommenter.getId());
        comment.setText("Qué voz");
        commentRepository.save(comment);

        List<Comment> result = commentRepository.findByToplineId(savedTopline.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Qué voz");
    }
}
