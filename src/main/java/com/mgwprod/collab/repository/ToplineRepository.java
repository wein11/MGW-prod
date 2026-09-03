package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Topline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ToplineRepository extends JpaRepository<Topline, Long> {
    List<Topline> findByBeatId(Long beatId);
    List<Topline> findByArtistId(Long artistId);
}
