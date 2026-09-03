package com.mgwprod.catalog.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

// TEMPORARY STUB — the real catalog module (Santiago+Mateo, docs/superpowers/plans/
// 2026-09-01-mgw-prod-catalog-module.md) is not merged to main yet, but collab needs
// Beat/BeatRepository to validate beatId (see docs/superpowers/plans/
// 2026-09-01-mgw-prod-collab-module.md, Task 3-4). Delete this whole file and let the
// real com.mgwprod.catalog.model.Beat take over as soon as that PR merges.
@Entity
@Table(name = "beats")
@Getter
@Setter
@NoArgsConstructor
public class Beat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "producer_id", nullable = false)
    private Long producerId;
}
