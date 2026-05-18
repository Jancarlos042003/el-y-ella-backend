# Integración con Mercado Pago

Guía de referencia basada en la integración real del proyecto **El y Ella Detalles**.
Aplica para proyectos universitarios, de prueba y producción.

---

## Variables de entorno requeridas

| Variable | Descripción |
|---|---|
| `MP_ACCESS_TOKEN` | Token de autenticación del SDK (prueba o producción) |
| `MP_WEBHOOK_SECRET` | Firma secreta para verificar notificaciones HMAC-SHA256 |
| `MP_NOTIFICATION_URL` | URL pública donde MP enviará los webhooks |

---

## Dónde obtener cada credencial

### Access Token
1. Entra a [developers.mercadopago.com](https://developers.mercadopago.com)
2. Selecciona tu aplicación
3. Sección **Credenciales** → copia el token según el modo:
   - Prueba: `TEST-xxx...`
   - Producción: `APP_USR-xxx...`

### Webhook Secret
El secret **no está en las credenciales generales** — se genera al configurar un webhook:

1. Panel de MP → tu aplicación → **Webhooks** → **Configurar notificaciones**
2. Ingresa tu URL de destino: `https://tu-dominio.com/api/v1/payments/webhook`
3. Selecciona el evento: **Pagos** ✓
4. Guarda → MP muestra la **firma secreta** (ese es el `MP_WEBHOOK_SECRET`)

> El secret es por webhook configurado. Si lo borras y recreas, cambia.

---

## Desarrollo local con ngrok

MP necesita una URL pública — nunca acepta `localhost`. Solución: ngrok.

### Instalación
```bash
sudo snap install ngrok
ngrok config add-authtoken TU_TOKEN_DE_NGROK
```

### Uso
```bash
# Terminal 1 — levantar el backend
mvn spring-boot:run

# Terminal 2 — exponer el puerto al mundo
ngrok http 8080
# Genera: https://a1b2-189-203-xx-xx.ngrok-free.app
```

### Configurar las variables con ngrok
```bash
export MP_ACCESS_TOKEN="TEST-xxx"
export MP_WEBHOOK_SECRET="firma-secreta-de-mp"
export MP_NOTIFICATION_URL="https://a1b2-189-203-xx-xx.ngrok-free.app/api/v1/payments/webhook"
```

> Cada vez que reinicias ngrok cambia la URL. Para evitarlo, activa un **dominio estático gratuito** en ngrok (1 dominio fijo disponible en el plan gratuito desde 2024):
> ```bash
> ngrok http --domain=tu-dominio-fijo.ngrok-free.app 8080
> ```

---

## Modo prueba vs Modo productivo

| | Modo prueba | Modo productivo |
|---|---|---|
| Access Token | `TEST-xxx` | `APP_USR-xxx` |
| Webhook secret | Generado en modo prueba | Generado en modo productivo |
| Pagos | Simulados (tarjetas de prueba) | Dinero real |
| Webhooks | Solo de transacciones simuladas | De compras reales |
| Requiere activación | No | Sí (datos fiscales + cuenta bancaria) |

Para proyectos universitarios o de demostración: **Modo prueba es suficiente**, incluso desplegado en la nube.

---

## Variables por entorno

### Desarrollo local (ngrok)
```bash
MP_ACCESS_TOKEN=TEST-xxx
MP_WEBHOOK_SECRET=secret-modo-prueba
MP_NOTIFICATION_URL=https://xxxx.ngrok-free.app/api/v1/payments/webhook
```

### Deploy en nube (GCP, Railway, Render, etc.) — aún en pruebas
```bash
MP_ACCESS_TOKEN=TEST-xxx          # mismo token de prueba
MP_WEBHOOK_SECRET=secret-modo-prueba
MP_NOTIFICATION_URL=https://tu-backend.run.app/api/v1/payments/webhook
```

### Producción real
```bash
MP_ACCESS_TOKEN=APP_USR-xxx       # token productivo
MP_WEBHOOK_SECRET=secret-modo-productivo
MP_NOTIFICATION_URL=https://api.tudominio.com/api/v1/payments/webhook
```

---

## Regla crítica del webhook: siempre responder 200 OK

MP interpreta `200 OK` como **"recibí la notificación"** — no como "la procesé con éxito".

Si el servidor responde cualquier otro código (4xx, 5xx), MP **reintenta el webhook** varias veces, lo que puede causar procesamiento duplicado de pagos.

**Implementación correcta en Spring Boot:**
```java
@PostMapping("/webhook")
public ResponseEntity<Void> handleWebhook(...) {
    try {
        paymentService.processWebhook(dataId, xRequestId, xSignature);
    } catch (Exception e) {
        // 200 OK significa: "recibí la notificación".
        // Siempre se responde 200 a MP para evitar reintentos indefinidos.
        log.error("Error al procesar webhook: {}", e.getMessage(), e);
    }
    return ResponseEntity.ok().build();
}
```

---

## Verificación de firma HMAC-SHA256

MP firma cada notificación en el header `x-signature` con el formato:
```
ts=1234567890,v1=abc123...
```

El payload que se firma es:
```
id:<data.id>;request-id:<x-request-id>;ts:<ts>;
```

**Implementación:**
```java
String manifest = "id:" + dataId + ";request-id:" + xRequestId + ";ts:" + ts + ";";
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(webhookSecret.getBytes(UTF_8), "HmacSHA256"));
String computed = HexFormat.of().formatHex(mac.doFinal(manifest.getBytes(UTF_8)));
// Comparar computed con v1 usando MessageDigest.isEqual (evita timing attacks)
```

> Si la firma no coincide → rechazar la notificación (no procesarla).

---

## Flujo completo de un pago

```
Usuario → POST /checkout
    └─ Backend crea Order + Payment en BD
    └─ Backend llama a MP: PreferenceClient.create()
    └─ MP devuelve preferenceId + initPoint
    └─ Backend retorna initPoint al frontend

Usuario → abre initPoint en el navegador
    └─ Pantalla de pago de Mercado Pago
    └─ Usuario ingresa datos de tarjeta

MP procesa el pago
    └─ MP llama POST /webhook?data.id=<paymentId>&type=payment
    └─ Backend verifica firma HMAC
    └─ Backend consulta el pago: PaymentClient.get(paymentId)
    └─ Backend actualiza estado del pedido (SHIPPED / CANCELLED)
    └─ Backend responde 200 OK a MP
```

---

## Tarjetas de prueba para demo

MP tiene tarjetas oficiales de sandbox. El **nombre del titular** controla el resultado.

> Verifica los números actualizados para tu país en:
> **developers.mercadopago.com → Herramientas de prueba → Tarjetas de prueba**

| Nombre del titular | Resultado |
|---|---|
| `APRO` | Pago aprobado ✅ |
| `OTHE` | Pago rechazado ❌ |
| `CONT` | Pago pendiente ⏳ |

---

## SDK de Java — dependencia Maven

```xml
<dependency>
    <groupId>com.mercadopago</groupId>
    <artifactId>sdk-java</artifactId>
    <version>2.9.2</version>
</dependency>
```

**Inicialización (una sola vez al arrancar):**
```java
@PostConstruct
public void init() {
    MercadoPagoConfig.setAccessToken(accessToken);
}
```
