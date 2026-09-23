package com.mgwprod.catalog.repository;

import com.mgwprod.catalog.model.BeatComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BeatCommentRepository extends JpaRepository<BeatComment, Long> {
    List<BeatComment> findByBeatId(Long beatId);
}
