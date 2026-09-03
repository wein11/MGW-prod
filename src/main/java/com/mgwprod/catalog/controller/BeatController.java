package com.mgwprod.catalog.controller;

import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.service.BeatService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/beats")
public class BeatController {

    private final BeatService beatService;

    public BeatController(BeatService beatService) {
        this.beatService = beatService;
    }

    @PostMapping
    public ResponseEntity<Beat> createBeat(@RequestAttribute(name = "userId", required = false) Long userId,
                                            @Valid @RequestBody Beat beat) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para publicar un beat");
        }
        Beat created = beatService.create(userId, beat);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Beat> listBeats(@RequestParam(required = false) String genre,
                                 @RequestParam(required = false) Integer bpm,
                                 @RequestParam(required = false) Long producerId) {
        return beatService.list(genre, bpm, producerId);
    }

    @GetMapping("/{id}")
    public Beat getBeat(@PathVariable Long id) {
        return beatService.getById(id);
    }
}
