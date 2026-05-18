package com.elyella.backend.config;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Provee el cliente de Google Cloud Storage usando Application Default Credentials.
 * En Cloud Run las credenciales se resuelven automáticamente desde la cuenta de servicio adjunta.
 * En local se requiere la variable GOOGLE_APPLICATION_CREDENTIALS apuntando a un JSON de clave.
 */
@Configuration
public class GcsConfig {

    @Bean
    public Storage storage() {
        return StorageOptions.getDefaultInstance().getService();
    }
}
