package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.service.CommentService;
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

// Endpoints anidados bajo /api/toplines/{toplineId}/comments.
@RestController
@RequestMapping("/api/toplines/{toplineId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    // POST /api/toplines/{toplineId}/comments -> comenta un topline (hay que estar logueado).
    @PostMapping
    public ResponseEntity<Comment> createComment(@PathVariable Long toplineId,
                                                  @RequestAttribute(name = "userId", required = false) Long userId,
                                                  @RequestBody Comment comment) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (comment.getText() == null || comment.getText().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        Comment created = commentService.create(toplineId, userId, comment);
        if (created == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/toplines/{toplineId}/comments -> lista los comentarios del topline.
    @GetMapping
    public ResponseEntity<List<Comment>> listComments(@PathVariable Long toplineId) {
        List<Comment> comments = commentService.listByTopline(toplineId);
        if (comments == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(comments);
    }
}
