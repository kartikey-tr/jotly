package com.jotly.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationData {

    private String name;
    private String email;
    private String profilePhoto;
    private String profilePhotoPublicId;
}