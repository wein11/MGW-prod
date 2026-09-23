package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Topline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToplineRepository extends JpaRepository<Topline, Long> {
    // Aunque el campo se llame `beat` (objeto Beat), Spring Data resuelve
    // findByBeatId como WHERE beat_id = ? sobre esa relación.
    List<Topline> findByBeatId(Long beatId);
    List<Topline> findByArtistId(Long artistId);
}
