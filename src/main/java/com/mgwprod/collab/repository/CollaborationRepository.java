package com.mgwprod.collab.repository;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CollaborationRepository extends JpaRepository<Collaboration, Long> {
    Optional<Collaboration> findByToplineId(Long toplineId);
    List<Collaboration> findByStatus(CollaborationStatus status);
}
