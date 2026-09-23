package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.Beat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

// Cada método define su propia consulta a partir del nombre — Spring Data arma el SQL
// automáticamente, no hace falta escribirlo.
public interface BeatRepository extends JpaRepository<Beat, Long> {
    // SELECT * FROM beats WHERE producer_id = ?
    List<Beat> findByProducerId(Long producerId);
    // SELECT * FROM beats WHERE genre = ?
    List<Beat> findByGenre(String genre);
    // SELECT * FROM beats WHERE bpm = ?
    List<Beat> findByBpm(Integer bpm);
    // Combina dos condiciones con AND — usado cuando el listado filtra por género y BPM a la vez.
    List<Beat> findByGenreAndBpm(String genre, Integer bpm);
}
