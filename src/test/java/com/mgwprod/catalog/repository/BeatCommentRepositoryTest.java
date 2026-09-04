package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.BeatComment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.test.context.TestPropertySource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.ANY;

@DataJpaTest
@AutoConfigureTestDatabase(replace = ANY)
@TestPropertySource(properties = "spring.jpa.hibernate.ddl-auto=create-drop")
class BeatCommentRepositoryTest {

    @Autowired
    private BeatCommentRepository beatCommentRepository;

    @Test
    void findByBeatIdReturnsOnlyThatBeatsComments() {
        BeatComment comment = new BeatComment();
        comment.setBeatId(1L);
        comment.setAuthorId(2L);
        comment.setText("Está buenísimo el beat");
        beatCommentRepository.save(comment);

        List<BeatComment> result = beatCommentRepository.findByBeatId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getText()).isEqualTo("Está buenísimo el beat");
    }
}
