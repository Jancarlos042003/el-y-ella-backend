package com.elyella.backend.service;

import com.elyella.backend.exception.ResourceNotFoundException;
import com.elyella.backend.model.User;
import com.elyella.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.openpdf.pdf.ITextRenderer;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio para la generación de reportes en PDF.
 * Usa la plantilla HTML en templates/reportes/ y openpdf-html para la conversión.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private static final DateTimeFormatter DISPLAY_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String TEMPLATE_PATH = "templates/reportes/reporte-usuarios.html";

    private final UserRepository userRepository;

    /**
     * Genera el reporte de usuarios en formato PDF.
     * Carga la plantilla HTML, inyecta los datos y convierte con openpdf-html.
     *
     * @return arreglo de bytes del PDF generado
     */
    @Transactional(readOnly = true)
    public byte[] generateUserReport() {
        List<User> users = userRepository.findAll();
        String html = buildHtml(users);
        return renderToPdf(html);
    }

    /**
     * Carga la plantilla HTML desde el classpath e inyecta los datos de usuarios.
     */
    private String buildHtml(List<User> users) {
        String template = loadTemplate();

        StringBuilder rows = new StringBuilder();
        for (User user : users) {
            String roleBadge = "ADMIN".equals(user.getRole().name())
                    ? "<span class=\"badge badge-admin\">ADMIN</span>"
                    : "<span class=\"badge badge-user\">USER</span>";

            String registeredAt = user.getCreatedAt() != null
                    ? user.getCreatedAt().format(DISPLAY_FORMATTER)
                    : "—";

            rows.append("<tr>")
                .append("<td>").append(user.getId()).append("</td>")
                .append("<td>").append(escapeHtml(user.getName())).append("</td>")
                .append("<td>").append(escapeHtml(user.getEmail())).append("</td>")
                .append("<td>").append(user.getPhone() != null ? escapeHtml(user.getPhone()) : "—").append("</td>")
                .append("<td>").append(roleBadge).append("</td>")
                .append("<td>").append(registeredAt).append("</td>")
                .append("</tr>");
        }

        String fecha = LocalDateTime.now().format(DISPLAY_FORMATTER);
        return template
                .replace("{{fecha}}", fecha)
                .replace("{{total}}", String.valueOf(users.size()))
                .replace("{{filas}}", rows.toString());
    }

    /**
     * Convierte el HTML resultante a PDF con ITextRenderer (openpdf-html 3.0.0).
     */
    private byte[] renderToPdf(String html) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(html);
            renderer.layout();
            renderer.createPDF(baos);
            log.debug("PDF de reporte de usuarios generado: {} bytes", baos.size());
            return baos.toByteArray();
        } catch (Exception e) {
            log.error("Error al generar PDF del reporte de usuarios: {}", e.getMessage(), e);
            throw new ResourceNotFoundException("No se pudo generar el reporte en PDF.");
        }
    }

    private String loadTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            return resource.getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("No se pudo cargar la plantilla del reporte: {}", TEMPLATE_PATH);
            throw new ResourceNotFoundException("Plantilla del reporte no encontrada.");
        }
    }

    /** Escapa caracteres HTML para prevenir inyección en la plantilla. */
    private String escapeHtml(String text) {
        if (text == null) return "—";
        return text.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;");
    }
}
