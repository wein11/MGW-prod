package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.service.CollaborationService;
import com.mgwprod.users.exception.UnauthenticatedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/collaborations")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    @PutMapping("/{id}")
    public Collaboration decide(@PathVariable Long id,
                                 @RequestAttribute(name = "userId", required = false) Long userId,
                                 @RequestParam CollaborationStatus status) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para decidir una colaboración");
        }
        return collaborationService.decide(id, userId, status);
    }

    @GetMapping
    public List<Collaboration> list(@RequestParam(required = false) CollaborationStatus status) {
        return collaborationService.listByStatus(status);
    }
}
