package com.mgwprod.collab.service;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Lógica de negocio de los comentarios sobre un topline: crear y listar.
@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ToplineService toplineService;

    public CommentService(CommentRepository commentRepository, ToplineService toplineService) {
        this.commentRepository = commentRepository;
        this.toplineService = toplineService;
    }

    // Devuelve null si el topline no existe.
    @Transactional
    public Comment create(Long toplineId, Long authorId, Comment comment) {
        Topline topline = toplineService.getById(toplineId);
        if (topline == null) {
            return null;
        }
        comment.setToplineId(toplineId);
        comment.setAuthorId(authorId);
        return commentRepository.save(comment);
    }

    // Devuelve null si el topline no existe.
    @Transactional(readOnly = true)
    public List<Comment> listByTopline(Long toplineId) {
        Topline topline = toplineService.getById(toplineId);
        if (topline == null) {
            return null;
        }
        return commentRepository.findByToplineId(toplineId);
    }
}
