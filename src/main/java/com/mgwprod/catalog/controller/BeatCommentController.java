package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.BeatComment;
import com.mgwprod.catalog.service.BeatCommentService;
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
                                                       @RequestBody BeatComment comment) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (comment.getText() == null || comment.getText().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        BeatComment created = beatCommentService.create(beatId, userId, comment);
        if (created == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public ResponseEntity<List<BeatComment>> listComments(@PathVariable Long beatId) {
        List<BeatComment> comments = beatCommentService.listByBeat(beatId);
        if (comments == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(comments);
    }
}
