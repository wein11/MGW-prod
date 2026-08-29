package com.mgwprod.users.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserRequest {
    private String displayName;
    private String city;
    private String genres;
    private Integer bpmMin;
    private Integer bpmMax;
    private String experienceLevel;
    private String bio;
}
