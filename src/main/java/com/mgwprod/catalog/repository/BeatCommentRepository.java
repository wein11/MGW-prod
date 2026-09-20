package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.BeatComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatCommentRepository extends JpaRepository<BeatComment, Long> {
    // Aunque el campo de la entidad se llame `beat` (un objeto Beat) y no `beatId`,
    // Spring Data igual entiende findByBeatId como "el id del beat relacionado", y
    // genera WHERE beat_id = ?.
    List<BeatComment> findByBeatId(Long beatId);
}
