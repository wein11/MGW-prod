package com.mgwprod.users.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "producer_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProducerProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String genres;

    @Column(name = "bpm_min")
    private Integer bpmMin;

    @Column(name = "bpm_max")
    private Integer bpmMax;

    @Column(name = "experience_level")
    private String experienceLevel;
}
