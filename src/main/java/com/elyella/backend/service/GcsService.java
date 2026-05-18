package com.elyella.backend.service;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;

/**
 * Gestión de imágenes en Google Cloud Storage.
 * El bucket usa uniform bucket-level access; la visibilidad pública se controla
 * vía IAM. La URL pública sigue el patrón https://storage.googleapis.com/{bucket}/{objeto}.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GcsService {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final String FOLDER = "flores/";

    private final Storage storage;

    @Value("${gcs.bucket-name}")
    private String bucketName;

    public String uploadImage(MultipartFile file) {
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Tipo de archivo no permitido. Se aceptan: JPEG, PNG, WebP.");
        }

        String objectName = FOLDER + UUID.randomUUID() + "_" + file.getOriginalFilename();
        BlobId blobId = BlobId.of(bucketName, objectName);
        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                .setContentType(contentType)
                .build();

        try {
            // El bucket usa uniform bucket-level access — el acceso público se gestiona
            // vía IAM (allUsers con rol Storage Object Viewer), no con ACL por objeto.
            storage.create(blobInfo, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Error al subir la imagen a GCS.", e);
        }

        String publicUrl = "https://storage.googleapis.com/" + bucketName + "/" + objectName;
        log.debug("Imagen subida a GCS: {}", publicUrl);
        return publicUrl;
    }

    public void deleteImage(String publicUrl) {
        String prefix = "https://storage.googleapis.com/" + bucketName + "/";
        if (!publicUrl.startsWith(prefix)) {
            log.warn("URL de imagen no pertenece al bucket configurado: {}", publicUrl);
            return;
        }
        String objectName = publicUrl.substring(prefix.length());
        boolean deleted = storage.delete(BlobId.of(bucketName, objectName));
        log.debug("Imagen eliminada de GCS: {} → {}", objectName, deleted);
    }
}
