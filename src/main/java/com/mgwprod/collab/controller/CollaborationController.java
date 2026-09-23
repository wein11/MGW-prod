package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Collaboration;
import com.mgwprod.collab.model.CollaborationStatus;
import com.mgwprod.collab.service.CollaborationService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints REST de las propuestas de colaboración: decidir (aceptar/rechazar),
// listar por estado, y borrar. No hay POST acá porque una Collaboration siempre nace
// automáticamente al crear un Topline (ver ToplineService.create).
@RestController
@RequestMapping("/api/collaborations")
public class CollaborationController {

    private final CollaborationService collaborationService;

    public CollaborationController(CollaborationService collaborationService) {
        this.collaborationService = collaborationService;
    }

    // El status llega como query param (?status=ACCEPTED) y Spring lo convierte
    // directo al enum CollaborationStatus.
    @PutMapping("/{id}")
    public ResponseEntity<Collaboration> decide(@PathVariable Long id,
                                                 @RequestAttribute(name = "userId", required = false) Long userId,
                                                 @RequestParam CollaborationStatus status) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Collaboration collaboration = collaborationService.getById(id);
        if (collaboration == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!collaborationService.canDecide(collaboration, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(collaborationService.decide(id, status));
    }

    @GetMapping
    public List<Collaboration> list(@RequestParam(required = false) CollaborationStatus status) {
        return collaborationService.listByStatus(status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteCollaboration(@PathVariable Long id,
                                                     @RequestAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Collaboration collaboration = collaborationService.getById(id);
        if (collaboration == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!collaborationService.canDelete(collaboration, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        collaborationService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
