package com.elyella.backend.controller;

import com.elyella.backend.service.GcsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Endpoints para subida de imágenes a Google Cloud Storage.
 * Flujo esperado en el frontend: (1) subir la imagen aquí para obtener la URL pública,
 * (2) enviar esa URL en el campo imageUrl al crear o editar una flor.
 */
@RestController
@RequestMapping("/api/v1/images")
@RequiredArgsConstructor
@Tag(name = "Imágenes", description = "Subida de imágenes a Google Cloud Storage")
public class ImageController {

    private final GcsService gcsService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Auth")
    @Operation(
            summary = "Subir imagen (ADMIN)",
            description = "Sube un archivo de imagen a GCS y retorna su URL pública para usarla en imageUrl de una flor."
    )
    public ResponseEntity<Map<String, String>> upload(@RequestParam("file") MultipartFile file) {
        String url = gcsService.uploadImage(file);
        return ResponseEntity.ok(Map.of("url", url));
    }
}
