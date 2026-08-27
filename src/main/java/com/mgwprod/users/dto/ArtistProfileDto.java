package com.mgwprod.users.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ArtistProfileDto {
    private final String genres;
    private final String bio;
}
