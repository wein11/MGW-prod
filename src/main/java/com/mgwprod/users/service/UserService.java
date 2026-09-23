package com.mgwprod.users.service;

import com.mgwprod.users.model.ArtistProfile;
import com.mgwprod.users.model.Role;
import com.mgwprod.users.model.User;
import com.mgwprod.users.repository.ArtistProfileRepository;
import com.mgwprod.users.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Se encarga de todo lo relacionado a un usuario ya registrado: leerlo, actualizar sus
// datos de perfil, verificar artistas, borrar la cuenta. Cada chequeo de "¿está
// permitido esto?" que necesitan UserController/ArtistVerificationController vive acá
// como un método booleano simple, sin lanzar excepciones.
@Service
public class UserService {

    private final UserRepository userRepository;
    private final ArtistProfileRepository artistProfileRepository;

    public UserService(UserRepository userRepository, ArtistProfileRepository artistProfileRepository) {
        this.userRepository = userRepository;
        this.artistProfileRepository = artistProfileRepository;
    }

    // Devuelve null en vez de lanzar una excepción cuando el id no existe — el
    // controller chequea `== null` y devuelve el 404 él mismo.
    @Transactional(readOnly = true)
    public User getById(Long userId) {
        return userRepository.findById(userId).orElse(null);
    }

    @Transactional(readOnly = true)
    public boolean isArtist(User user) {
        return user.getRole() == Role.ARTIST;
    }

    @Transactional(readOnly = true)
    public boolean isAdmin(User user) {
        return user.getRole() == Role.ADMIN;
    }

    // El controller ya validó que el usuario exista y sea artista antes de llamar acá.
    @Transactional(readOnly = true)
    public ArtistProfile getProfile(Long userId) {
        return artistProfileRepository.findByUserId(userId).orElse(null);
    }

    // "Owner" acá significa: ¿la persona que hace el request es la misma persona de la
    // que habla la URL? Sirve para que el usuario A no pueda editar/borrar la cuenta del B.
    @Transactional(readOnly = true)
    public boolean isOwner(Long targetUserId, Long requestingUserId) {
        return targetUserId.equals(requestingUserId);
    }

    // Update parcial: solo se cambian los campos que el cliente realmente mandó (no
    // nulos) — enviar {"city": "CABA"} solo no borra displayName.
    @Transactional
    public User updateUser(Long targetUserId, User request) {
        User user = getById(targetUserId);
        if (user == null) {
            return null;
        }
        if (request.getDisplayName() != null) {
            user.setDisplayName(request.getDisplayName());
        }
        if (request.getCity() != null) {
            user.setCity(request.getCity());
        }
        return userRepository.save(user);
    }

    // Misma idea de update parcial que updateUser, pero para los campos del perfil de
    // artista. orElseThrow() acá (no orElse(null)) es a propósito: el controller ya
    // confirmó que el usuario existe Y es artista, así que si el perfil no aparece acá
    // sería un estado inconsistente de la base, no un 404 normal.
    @Transactional
    public ArtistProfile updateArtistProfile(Long targetUserId, ArtistProfile request) {
        ArtistProfile profile = artistProfileRepository.findByUserId(targetUserId).orElseThrow();
        if (request.getGenres() != null) {
            profile.setGenres(request.getGenres());
        }
        if (request.getBio() != null) {
            profile.setBio(request.getBio());
        }
        if (request.getBpmMin() != null) {
            profile.setBpmMin(request.getBpmMin());
        }
        if (request.getBpmMax() != null) {
            profile.setBpmMax(request.getBpmMax());
        }
        if (request.getExperienceLevel() != null) {
            profile.setExperienceLevel(request.getExperienceLevel());
        }
        return artistProfileRepository.save(profile);
    }

    // Mismo razonamiento que arriba: solo se llega acá después de que
    // ArtistVerificationController ya confirmó que el destino es un artista.
    @Transactional
    public ArtistProfile verifyArtist(Long artistId) {
        ArtistProfile profile = artistProfileRepository.findByUserId(artistId).orElseThrow();
        profile.setVerified(true);
        return artistProfileRepository.save(profile);
    }

    // Devuelve false si no se pudo borrar porque el usuario tiene contenido asociado.
    // deleteById + flush (en vez de solo deleteById) obliga a Hibernate a ejecutar el
    // DELETE ahora mismo, dentro de este try, en vez de al final de la transacción —
    // si no, la DataIntegrityViolationException aparecería más tarde, afuera de este
    // método, donde ya no la podríamos atrapar y convertir en `false`.
    @Transactional
    public boolean delete(Long targetUserId) {
        try {
            userRepository.deleteById(targetUserId);
            userRepository.flush();
            return true;
        } catch (DataIntegrityViolationException ex) {
            return false;
        }
    }
}
