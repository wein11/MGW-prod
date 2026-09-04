package com.mgwprod.collab.service;

import com.mgwprod.collab.model.Comment;
import com.mgwprod.collab.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CommentService {

    private final CommentRepository commentRepository;
    private final ToplineService toplineService;

    public CommentService(CommentRepository commentRepository, ToplineService toplineService) {
        this.commentRepository = commentRepository;
        this.toplineService = toplineService;
    }

    @Transactional
    public Comment create(Long toplineId, Long authorId, Comment comment) {
        toplineService.getById(toplineId);
        comment.setToplineId(toplineId);
        comment.setAuthorId(authorId);
        return commentRepository.save(comment);
    }

    @Transactional(readOnly = true)
    public List<Comment> listByTopline(Long toplineId) {
        toplineService.getById(toplineId);
        return commentRepository.findByToplineId(toplineId);
    }
}
