package com.mgwprod.challenges.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "votes")
@Getter
@Setter
@NoArgsConstructor
public class Vote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // submissionId y voterId los setea el VoteService desde el path y el userId
    // autenticado, no vienen en el body. Sin @NotNull para no romper el @Valid
    // @RequestBody del controller; integridad vía @Column(nullable = false) + service.
    @Column(name = "submission_id", nullable = false)
    private Long submissionId;

    @Column(name = "voter_id", nullable = false)
    private Long voterId;

    @NotNull(message = "El puntaje es obligatorio")
    @Min(value = 1, message = "El puntaje mínimo es 1")
    @Max(value = 10, message = "El puntaje máximo es 10")
    @Column(nullable = false)
    private Integer score;

    @Column(length = 1000)
    private String comment;
}
