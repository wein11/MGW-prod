package com.mgwprod.challenges.controller;

import com.mgwprod.challenges.model.Challenge;
import com.mgwprod.challenges.service.ChallengeService;
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

// Endpoints REST de los challenges: crear, listar, ver, editar, borrar y elegir el
// opportunity pick.
@RestController
@RequestMapping("/api/challenges")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    // Además de validar los campos propios del challenge, hay que validar que el
    // guestArtistId realmente exista (404) y sea un artista (403) — por eso acá hay
    // más pasos que en un create común.
    @PostMapping
    public ResponseEntity<Challenge> createChallenge(@RequestAttribute(name = "userId", required = false) Long userId,
                                                      @RequestBody Challenge challenge) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        if (challenge.getTitle() == null || challenge.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (challenge.getGenre() == null || challenge.getGenre().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        if (challenge.getBpm() == null || challenge.getBpm() < 1) {
            return ResponseEntity.badRequest().body(null);
        }
        if (challenge.getDeadline() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        if (challenge.getGuestArtistId() == null) {
            return ResponseEntity.badRequest().body(null);
        }
        if (!challengeService.canCreateChallenge(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (!challengeService.userExists(challenge.getGuestArtistId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!challengeService.isArtist(challenge.getGuestArtistId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        Challenge created = challengeService.create(userId, challenge);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    // GET /api/challenges -> lista todos los challenges (público).
    @GetMapping
    public List<Challenge> listChallenges() {
        return challengeService.list();
    }

    // GET /api/challenges/{id} -> devuelve un challenge o 404.
    @GetMapping("/{id}")
    public ResponseEntity<Challenge> getChallenge(@PathVariable Long id) {
        Challenge challenge = challengeService.getById(id);
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        return ResponseEntity.ok(challenge);
    }

    // Un challenge ya cerrado (con resultados calculados) no se puede editar más —
    // cambiar el deadline o el título después de haber repartido premios no tendría sentido.
    @PutMapping("/{id}")
    public ResponseEntity<Challenge> updateChallenge(@PathVariable Long id,
                                                      @RequestAttribute(name = "userId", required = false) Long userId,
                                                      @RequestBody Challenge challenge) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Challenge existing = challengeService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!challengeService.canModify(existing, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (challengeService.isClosed(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        if (challenge.getTitle() != null && challenge.getTitle().isBlank()) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(challengeService.update(id, challenge));
    }

    // Mismo chequeo de "no tocar un challenge ya cerrado" que en update.
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteChallenge(@PathVariable Long id,
                                                 @RequestAttribute(name = "userId", required = false) Long userId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        Challenge existing = challengeService.getById(id);
        if (existing == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }
        if (!challengeService.canModify(existing, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        if (challengeService.isClosed(id)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        challengeService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // Solo el artista invitado (jurado) puede elegir su favorita entre las submissions.
    @PutMapping("/{id}/opportunity-pick")
    public ResponseEntity<Challenge> opportunityPick(@PathVariable Long id,
                                                      @RequestAttribute(name = "userId", required = false) Long userId,
                                                      @RequestParam Long submissionId) {
        if (userId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }
        Challenge challenge = challengeService.getById(id);
        if (challenge == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }
        if (!challengeService.isGuestArtist(challenge, userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }
        // null acá significa "esa submission no pertenece a este challenge" — un 400,
        // no un 404, porque el submissionId sí existe, solo que no corresponde acá.
        Challenge updated = challengeService.setOpportunityPick(challenge, submissionId);
        if (updated == null) {
            return ResponseEntity.badRequest().body(null);
        }
        return ResponseEntity.ok(updated);
    }
}
