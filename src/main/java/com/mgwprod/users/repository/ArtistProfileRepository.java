package com.mgwprod.users.repository;

import com.mgwprod.users.model.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    // findByUserId funciona aunque el campo de la entidad se llame `user` (un objeto
    // User) y no `userId` — Spring Data lo interpreta como "el id de la relación user",
    // o sea WHERE user_id = ?.
    Optional<ArtistProfile> findByUserId(Long userId);

    // Sirve para un futuro listado de "artistas verificados"; verified arranca en
    // false y solo pasa a true a través de UserService.verifyArtist.
    List<ArtistProfile> findByVerifiedTrue();
}
