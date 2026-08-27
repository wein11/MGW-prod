package com.mgwprod.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ProducerProfileDto {
    private final String genres;
    private final Integer bpmMin;
    private final Integer bpmMax;
    private final String experienceLevel;
}
