package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.service.BeatCommentService;
import com.mgwprod.users.exception.UnauthenticatedException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beats/{beatId}/comments")
public class BeatCommentController {

    private final BeatCommentService beatCommentService;

    public BeatCommentController(BeatCommentService beatCommentService) {
        this.beatCommentService = beatCommentService;
    }

    @PostMapping
    public ResponseEntity<BeatComment> createComment(@PathVariable Long beatId,
                                                       @RequestAttribute(name = "userId", required = false) Long userId,
                                                       @Valid @RequestBody BeatComment comment) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para comentar");
        }
        BeatComment created = beatCommentService.create(beatId, userId, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<BeatComment> listComments(@PathVariable Long beatId) {
        return beatCommentService.listByBeat(beatId);
    }
}
