package com.mgwprod.users.repository;

import com.mgwprod.users.model.ProducerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProducerProfileRepository extends JpaRepository<ProducerProfile, Long> {
    Optional<ProducerProfile> findByUserId(Long userId);

    List<ProducerProfile> findByVerifiedTrue();
}
