package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.service.BeatService;
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

// Endpoints REST del catálogo de beats: publicar, listar/filtrar, ver, editar y borrar.
@RestController
@RequestMapping("/api/beats")
public class BeatController {

    private final BeatService beatService;

    public BeatController(BeatService beatService) {
        this.beatService = beatService;
    }

    // Orden de chequeos: 401 (login) -> 400 (campos) -> 403 (rol/límite). Los campos se
    // validan antes que el rol porque no tiene sentido consultar el límite de
    // producciones de un beat que ni siquiera está bien formado.
    @PostMapping
    public ResponseEntity<Beat> createBeat(@RequestAttribute(name = "userId", required = false) Long userId,
                                            @RequestBody Beat beat) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (beat.getTitle() == null || beat.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (beat.getGenre() == null || beat.getGenre().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (beat.getBpm() == null || beat.getBpm() < 1) {
            return ResponseEntity.badRequest().body(null);
        }
        if (beat.getAudioUrl() == null || beat.getAudioUrl().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!beatService.isArtist(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (beatService.isAtProductionLimit(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Beat created = beatService.create(userId, beat);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // Endpoint público, sin login. Los tres query params son opcionales y se combinan
    // según la lógica de BeatService.list.
    @GetMapping
    public List<Beat> listBeats(@RequestParam(required = false) String genre,
                                 @RequestParam(required = false) Integer bpm,
                                 @RequestParam(required = false) Long producerId) {
        return beatService.list(genre, bpm, producerId);
    }

    // GET /api/beats/{id} -> devuelve un beat o 404 si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<Beat> getBeat(@PathVariable Long id) {
        Beat beat = beatService.getById(id);
        if (beat == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(beat);
    }

    // PUT /api/beats/{id} -> edita un beat. Solo el dueño o un admin (si no, 403).
    @PutMapping("/{id}")
    public ResponseEntity<Beat> updateBeat(@PathVariable Long id,
                                            @RequestAttribute(name = "userId", required = false) Long userId,
                                            @RequestBody Beat beat) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Beat existing = beatService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!beatService.canModify(existing, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        return ResponseEntity.ok(beatService.update(id, beat));
    }

    // DELETE /api/beats/{id} -> borra un beat. Solo el dueño o un admin. Devuelve 204 sin body.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBeat(@PathVariable Long id,
                                            @RequestAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Beat existing = beatService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!beatService.canModify(existing, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        beatService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
