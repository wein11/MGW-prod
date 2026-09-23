package com.mgwprod.catalog.service;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.repository.BeatCommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BeatCommentService {

    private final BeatCommentRepository beatCommentRepository;
    private final BeatService beatService;

    public BeatCommentService(BeatCommentRepository beatCommentRepository, BeatService beatService) {
        this.beatCommentRepository = beatCommentRepository;
        this.beatService = beatService;
    }

    @Transactional
    public BeatComment create(Long beatId, Long authorId, BeatComment comment) {
        Beat beat = beatService.getById(beatId);
        if (beat == null) {
            return null;
        }
        comment.setBeat(beat);
        comment.setAuthorId(authorId);
        return beatCommentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<BeatComment> listByBeat(Long beatId) {
        Beat beat = beatService.getById(beatId);
        if (beat == null) {
            return null;
        }
        return beatCommentRepository.findByBeatId(beatId);
    }
}
