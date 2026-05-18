package com.elyella.backend.controller;

import com.elyella.backend.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoints de generación de reportes en PDF.
 * Acceso restringido a ADMIN (configurado en SecurityConfig).
 */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@SecurityRequirement(name = "Bearer Auth")
@Tag(name = "Reportes", description = "Generación de reportes en PDF — solo ADMIN")
public class ReportController {

    private final ReportService reportService;

    @GetMapping(value = "/users", produces = MediaType.APPLICATION_PDF_VALUE)
    @Operation(
            summary = "Reporte de usuarios (PDF)",
            description = "Genera y descarga un PDF con el listado completo de usuarios registrados.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "PDF generado correctamente",
                            content = @Content(mediaType = "application/pdf")),
                    @ApiResponse(responseCode = "403", description = "Acceso denegado")
            }
    )
    public ResponseEntity<byte[]> getUserReport() {
        byte[] pdf = reportService.generateUserReport();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "reporte-usuarios.pdf");
        headers.setContentLength(pdf.length);

        return ResponseEntity.ok().headers(headers).body(pdf);
    }
}
