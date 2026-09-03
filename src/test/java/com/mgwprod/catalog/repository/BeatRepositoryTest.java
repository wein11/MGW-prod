package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.Beat;
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
class BeatRepositoryTest {

    @Autowired
    private BeatRepository beatRepository;

    @Test
    void findByProducerIdReturnsOnlyThatProducersBeats() {
        Beat beat = new Beat();
        beat.setProducerId(1L);
        beat.setTitle("Trap Beat");
        beat.setGenre("Trap");
        beat.setBpm(140);
        beat.setAudioUrl("https://soundcloud.com/example/trap-beat");
        beatRepository.save(beat);

        List<Beat> result = beatRepository.findByProducerId(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getTitle()).isEqualTo("Trap Beat");
    }
}
