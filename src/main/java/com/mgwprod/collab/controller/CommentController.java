package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.service.CommentService;
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
@RequestMapping("/api/toplines/{toplineId}/comments")
public class CommentController {

    private final CommentService commentService;

    public CommentController(CommentService commentService) {
        this.commentService = commentService;
    }

    @PostMapping
    public ResponseEntity<Comment> createComment(@PathVariable Long toplineId,
                                                  @RequestAttribute(name = "userId", required = false) Long userId,
                                                  @Valid @RequestBody Comment comment) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para comentar");
        }
        Comment created = commentService.create(toplineId, userId, comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Comment> listComments(@PathVariable Long toplineId) {
        return commentService.listByTopline(toplineId);
    }
}
