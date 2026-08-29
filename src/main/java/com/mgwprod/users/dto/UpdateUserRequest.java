package com.mgwprod.users.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    // Every field is optional: null means "don't change this field", so @NotBlank
    // (which rejects null) is not used here — only @Size(min = 1) to reject an
    // explicitly-sent empty string while still letting omission (null) pass through.
    @Size(min = 1, message = "El nombre a mostrar no puede estar vacío")
    private String displayName;

    @Size(min = 1, message = "La ciudad no puede estar vacía")
    private String city;

    @Size(min = 1, message = "Los géneros no pueden estar vacíos")
    private String genres;

    @Min(value = 1, message = "El BPM mínimo debe ser mayor a 0")
    private Integer bpmMin;

    @Min(value = 1, message = "El BPM máximo debe ser mayor a 0")
    private Integer bpmMax;

    @Size(min = 1, message = "El nivel de experiencia no puede estar vacío")
    private String experienceLevel;

    @Size(min = 1, message = "La biografía no puede estar vacía")
    private String bio;
}
