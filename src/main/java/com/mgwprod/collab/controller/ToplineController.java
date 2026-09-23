package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.service.ToplineService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Endpoints REST de toplines: crear, listar/filtrar, ver, editar y borrar.
@RestController
@RequestMapping("/api/toplines")
public class ToplineController {

    private final ToplineService toplineService;

    public ToplineController(ToplineService toplineService) {
        this.toplineService = toplineService;
    }

    // El 404 acá viene después de los chequeos de rol/límite, porque recién adentro
    // de toplineService.create se termina de confirmar si el beat existe.
    @PostMapping
    public ResponseEntity<Topline> createTopline(@RequestAttribute(name = "userId", required = false) Long userId,
                                                  @RequestBody Topline topline) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (topline.getBeat() == null || topline.getBeat().getId() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        if (topline.getAudioUrl() == null || topline.getAudioUrl().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!toplineService.isArtist(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (toplineService.isAtProductionLimit(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Topline created = toplineService.create(userId, topline);
        if (created == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/toplines?beatId=X o ?artistId=Y -> lista toplines, con filtro opcional.
    @GetMapping
    public List<Topline> listToplines(@RequestParam(required = false) Long beatId,
                                       @RequestParam(required = false) Long artistId) {
        return toplineService.list(beatId, artistId);
    }

    // GET /api/toplines/{id} -> devuelve un topline o 404.
    @GetMapping("/{id}")
    public ResponseEntity<Topline> getTopline(@PathVariable Long id) {
        Topline topline = toplineService.getById(id);
        if (topline == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(topline);
    }

    // PUT /api/toplines/{id} -> edita un topline. Solo el artista que lo subió o un admin.
    @PutMapping("/{id}")
    public ResponseEntity<Topline> updateTopline(@PathVariable Long id,
                                                  @RequestAttribute(name = "userId", required = false) Long userId,
                                                  @RequestBody Topline topline) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Topline existing = toplineService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!toplineService.canModify(existing, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(toplineService.update(id, topline));
    }

    // DELETE /api/toplines/{id} -> borra un topline. Solo el artista que lo subió o un admin.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTopline(@PathVariable Long id,
                                               @RequestAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Topline existing = toplineService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!toplineService.canModify(existing, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        toplineService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
