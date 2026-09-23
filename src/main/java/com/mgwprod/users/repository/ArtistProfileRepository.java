package com.mgwprod.users.repository;

import com.mgwprod.users.model.ArtistProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ArtistProfileRepository extends JpaRepository<ArtistProfile, Long> {
    Optional<ArtistProfile> findByUserId(Long userId);

    List<ArtistProfile> findByVerifiedTrue();
}
