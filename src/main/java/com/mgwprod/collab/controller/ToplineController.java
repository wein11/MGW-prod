package com.mgwprod.collab.controller;

import com.mgwprod.collab.model.Topline;
import com.mgwprod.collab.service.ToplineService;
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
@RequestMapping("/api/toplines")
public class ToplineController {

    private final ToplineService toplineService;

    public ToplineController(ToplineService toplineService) {
        this.toplineService = toplineService;
    }

    @PostMapping
    public ResponseEntity<Topline> createTopline(@RequestAttribute(name = "userId", required = false) Long userId,
                                                  @Valid @RequestBody Topline topline) {
        if (userId == null) {
            throw new UnauthenticatedException("Necesitás iniciar sesión para subir un topline");
        }
        Topline created = toplineService.create(userId, topline);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping
    public List<Topline> listToplines(@RequestParam(required = false) Long beatId,
                                       @RequestParam(required = false) Long artistId) {
        return toplineService.list(beatId, artistId);
    }

    @GetMapping("/{id}")
    public Topline getTopline(@PathVariable Long id) {
        return toplineService.getById(id);
    }
}
