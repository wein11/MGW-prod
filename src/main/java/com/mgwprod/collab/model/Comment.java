package com.mgwprod.collab.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "comments")
@Getter
@Setter
@NoArgsConstructor
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Server-derived (path variable + authenticated session, see CommentService.create)
    // — same pattern as Topline.artistId: never accepted from client JSON, so no
    // @NotNull here since @Valid runs before the service sets these.
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "topline_id", nullable = false)
    private Long toplineId;

    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @NotBlank(message = "El comentario no puede estar vacío")
    @Column(nullable = false, length = 1000)
    private String text;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }
}
