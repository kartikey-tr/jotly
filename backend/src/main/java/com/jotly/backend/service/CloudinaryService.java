package com.jotly.backend.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Service
public class CloudinaryService {

    private final Cloudinary cloudinary;

    @Value("${jotly.default-profile-photo}")
    private String defaultProfilePhoto;

    public CloudinaryService(Cloudinary cloudinary) {
        this.cloudinary = cloudinary;
    }

    public CloudinaryUploadResult uploadProfilePhoto(MultipartFile file) {

        // No photo provided → use default photo
        if (file == null || file.isEmpty()) {
            return new CloudinaryUploadResult(
                    defaultProfilePhoto,
                    null
            );
        }

        try {

            Map<?, ?> result = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "jotly/profile-photos",
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");

            return new CloudinaryUploadResult(
                    secureUrl,
                    publicId
            );

        } catch (IOException e) {
            throw new RuntimeException(
                    "Failed to upload profile photo",
                    e
            );
        }
    }

    public record CloudinaryUploadResult(
            String secureUrl,
            String publicId
    ) {
    }
}