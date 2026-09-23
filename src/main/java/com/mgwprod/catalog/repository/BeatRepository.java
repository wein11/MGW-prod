package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.Beat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatRepository extends JpaRepository<Beat, Long> {
    List<Beat> findByProducerId(Long producerId);
    List<Beat> findByGenre(String genre);
    List<Beat> findByBpm(Integer bpm);
    List<Beat> findByGenreAndBpm(String genre, Integer bpm);
}
