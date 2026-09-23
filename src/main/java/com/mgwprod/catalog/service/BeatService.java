package com.mgwprod.catalog.service;

import com.mgwprod.billing.service.SubscriptionService;
import com.mgwprod.catalog.model.Beat;
import com.mgwprod.catalog.repository.BeatRepository;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

// Toda la lógica de negocio sobre beats: crear, listar con filtros, actualizar, borrar,
// y los chequeos de permisos que necesita BeatController antes de cada operación.
@Service
public class BeatService {

    private final BeatRepository beatRepository;
    private final UserRepository userRepository;
    private final SubscriptionService subscriptionService;

    public BeatService(BeatRepository beatRepository, UserRepository userRepository,
                        SubscriptionService subscriptionService) {
        this.beatRepository = beatRepository;
        this.userRepository = userRepository;
        this.subscriptionService = subscriptionService;
    }

    // El controller llama esto antes de crear, para poder devolver 403 sin
    // ambigüedad con el 404 de "productor no existe".
    @Transactional(readOnly = true)
    public boolean isArtist(Long userId) {
        User user = userRepository.findById(userId).orElse(null);
        return user != null && user.getRole() == Role.ARTIST;
    }

    // El controller la usa para devolver 403 antes de crear.
    // No puede ser readOnly: por dentro (vía SubscriptionService) puede terminar
    // creando la suscripción del usuario si todavía no tenía una — una transacción
    // de solo lectura no deja hacer ese INSERT y la request explota con 500.
    @Transactional
    public boolean isAtProductionLimit(Long producerId) {
        return subscriptionService.isAtProductionLimit(producerId);
    }

    // Al crear un beat también se le avisa a SubscriptionService que este productor
    // sumó una producción más (cuenta para el límite del plan free).
    @Transactional
    public Beat create(Long producerId, Beat beat) {
        subscriptionService.recordProduction(producerId);
        beat.setProducerId(producerId);
        return beatRepository.save(beat);
    }

    // Filtra en cascada: si viene producerId se ignoran los demás filtros, si vienen
    // genre y bpm juntos se combinan, y si no viene nada se devuelven todos.
    @Transactional(readOnly = true)
    public List<Beat> list(String genre, Integer bpm, Long producerId) {
        if (producerId != null) {
            return beatRepository.findByProducerId(producerId);
        }
        if (genre != null && bpm != null) {
            return beatRepository.findByGenreAndBpm(genre, bpm);
        }
        if (genre != null) {
            return beatRepository.findByGenre(genre);
        }
        if (bpm != null) {
            return beatRepository.findByBpm(bpm);
        }
        return beatRepository.findAll();
    }

    // Devuelve null si no existe — el controller decide el 404.
    @Transactional(readOnly = true)
    public Beat getById(Long id) {
        return beatRepository.findById(id).orElse(null);
    }

    // El controller ya validó los campos del request antes de llamar acá — este
    // método solo aplica el merge sobre el beat existente. Solo se pisa lo que vino
    // no nulo, así una actualización parcial no borra el resto de los campos.
    @Transactional
    public Beat update(Long id, Beat request) {
        Beat beat = getById(id);
        if (beat == null) {
            return null;
        }
        if (request.getTitle() != null) beat.setTitle(request.getTitle());
        if (request.getGenre() != null) beat.setGenre(request.getGenre());
        if (request.getBpm() != null) beat.setBpm(request.getBpm());
        if (request.getKey() != null) beat.setKey(request.getKey());
        if (request.getAudioUrl() != null) beat.setAudioUrl(request.getAudioUrl());
        return beatRepository.save(beat);
    }

    // Borra el beat por id. Los permisos ya los chequeó el controller con canModify.
    @Transactional
    public void delete(Long id) {
        beatRepository.deleteById(id);
    }

    // true si requestingUserId puede modificar/borrar el beat (dueño o admin).
    // El controller la usa para decidir si devuelve 403 antes de mutar nada.
    @Transactional(readOnly = true)
    public boolean canModify(Beat beat, Long requestingUserId) {
        if (beat.getProducerId().equals(requestingUserId)) {
            return true;
        }
        User requester = userRepository.findById(requestingUserId).orElse(null);
        return requester != null && requester.getRole() == Role.ADMIN;
    }
}
