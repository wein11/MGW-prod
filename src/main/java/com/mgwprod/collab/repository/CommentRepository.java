package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Comment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CommentRepository extends JpaRepository<Comment, Long> {
    List<Comment> findByToplineId(Long toplineId);
}
