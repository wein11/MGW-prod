package com.mgwprod.users.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
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

    @JsonIgnore
    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Size(min = 1, message = "Los géneros no pueden estar vacíos")
    private String genres;

    @Min(value = 1, message = "El BPM mínimo debe ser mayor a 0")
    @Column(name = "bpm_min")
    private Integer bpmMin;

    @Min(value = 1, message = "El BPM máximo debe ser mayor a 0")
    @Column(name = "bpm_max")
    private Integer bpmMax;

    @Size(min = 1, message = "El nivel de experiencia no puede estar vacío")
    @Column(name = "experience_level")
    private String experienceLevel;

    @Column(nullable = false)
    private boolean verified = false;
}
