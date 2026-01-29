# Service Email

## Descripcion

Servicio mock que simula la creacion de cuentas de correo corporativo. Es el **segundo paso** del proceso de onboarding.

## Puerto

| Entorno | Puerto |
|---------|--------|
| Desarrollo | 8082 |
| Docker | 10082 |

## Estructura del Proyecto

```
service-email/
├── src/main/java/com/poc/onboarding/email/
│   ├── ServiceEmailApplication.java
│   ├── controller/
│   │   └── EmailMockController.java
│   ├── entity/
│   │   └── PeticionEmail.java
│   ├── listener/
│   │   └── EmailRequestListener.java
│   ├── repository/
│   │   └── PeticionEmailRepository.java
│   └── service/
│       └── PeticionService.java
└── src/main/resources/
    ├── application.yml
    └── templates/
        └── peticiones.html
```

## Entidad Principal

### PeticionEmail

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | Long | ID autoincremental |
| peticionId | String | UUID unico de la peticion |
| workflowId | String | ID del workflow de Temporal |
| empleadoNombre | String | Nombre del empleado |
| empleadoEmail | String | Email personal del empleado |
| emailCorporativo | String | Email corporativo generado |
| estado | EstadoPeticion | PENDIENTE, APROBADA, DENEGADA |
| tipoOperacion | TipoOperacion | CREAR, ELIMINAR |
| motivoDenegacion | String | Motivo si fue denegada |
| fechaCreacion | LocalDateTime | Cuando se creo |
| fechaProcesamiento | LocalDateTime | Cuando se proceso |
| procesadoPor | String | Quien aprobo/denego |

### Generacion de Email Corporativo

El email corporativo se genera a partir del username de LDAP:
- Si username = `jgonzalez`
- Email corporativo = `jgonzalez@empresa.com`

```java
public void generarEmailCorporativo(String username) {
    if (username != null) {
        this.emailCorporativo = username + "@empresa.com";
    }
}
```

## Listeners RabbitMQ

### Cola de Peticiones

- **Cola**: `queue.email.request`
- **Routing Key**: `email.request`

Cuando llega una peticion:
1. Extrae username del paso LDAP anterior (de `event.getDatos().get("username")`)
2. Genera email corporativo propuesto
3. Crea registro en BD con estado PENDIENTE
4. Espera aprobacion manual

### Cola de Compensaciones

- **Cola**: `queue.email.compensate`
- **Routing Key**: `email.compensate`

Cuando llega una compensacion:
1. Busca la peticion original por `workflowId`
2. **ELIMINA el registro de la base de datos**
3. Envia respuesta de compensacion completada
4. No requiere intervencion manual (automatico)

```java
@RabbitListener(queues = RabbitMQConfig.QUEUE_EMAIL_COMPENSATE)
public void handleCompensation(PeticionCreatedEvent event) {
    // 1. Eliminar la peticion de la BD
    peticionService.eliminarPorWorkflowId(event.getWorkflowId());

    // 2. Enviar respuesta de compensacion completada
    PeticionResponseEvent response = PeticionResponseEvent.aprobada(...);
    rabbitTemplate.convertAndSend(...);
}
```

## Servicio de Peticiones

### Metodos Principales

| Metodo | Descripcion |
|--------|-------------|
| `obtenerPendientes()` | Lista peticiones pendientes |
| `obtenerHistorial()` | Lista peticiones procesadas |
| `aprobar(id, procesadoPor)` | Aprueba y notifica |
| `denegar(id, motivo, procesadoPor)` | Deniega y notifica |

### Datos Retornados al Aprobar

```java
Map<String, Object> datos = new HashMap<>();
datos.put("email_corporativo", peticion.getEmailCorporativo());
```

## Controller

### Endpoints

| Metodo | Ruta | Descripcion |
|--------|------|-------------|
| GET | `/` | Redirige a `/peticiones` |
| GET | `/peticiones` | Dashboard con pendientes e historial |
| POST | `/peticiones/{id}/aprobar` | Aprobar peticion |
| POST | `/peticiones/{id}/denegar` | Denegar peticion |

## Frontend (Thymeleaf)

### Diferencias con Service LDAP

En lugar de mostrar `usernameGenerado`, muestra `emailCorporativo`:

```html
<div th:if="${peticion.emailCorporativo}" class="mt-2">
    <small class="text-muted">Email Corp.:</small><br>
    <code>[[${peticion.emailCorporativo}]]</code>
</div>
```

## Configuracion

```yaml
server:
  port: 8082
  forward-headers-strategy: framework
  servlet:
    session:
      tracking-modes: cookie

spring:
  datasource:
    url: jdbc:mysql://localhost:10306/onboarding_email
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: localhost
    port: 10672

mock-service:
  name: Email Corporativo
  icon: bi-envelope
```

## Base de Datos

- **Schema**: `onboarding_email`
- **Tabla**: `peticion_email`

### Ciclo de Vida de los Datos

| Situacion | Accion en BD |
|-----------|--------------|
| Nueva peticion | `INSERT` con estado PENDIENTE |
| Usuario aprueba | `UPDATE` estado a APROBADA |
| Usuario deniega | `UPDATE` estado a DENEGADA + compensar LDAP |
| Compensacion (rollback) | `DELETE` del registro |

**En caso de rollback:** Si un paso posterior (Sistemas o Equipamiento) es denegado, este servicio recibe un mensaje de compensacion que **elimina el registro** de la BD.

## Dependencia con LDAP

Este servicio depende del paso LDAP:
- Necesita el username generado por LDAP para crear el email corporativo
- Si LDAP fue denegado, este paso nunca se ejecuta
- Si este paso es denegado, se ejecuta compensacion en LDAP

## Flujo Completo

```
LDAP aprobado (username: jgonzalez)
        │
        ▼
PeticionCreatedEvent llega a Email
        │
        ├── datos.username = "jgonzalez"
        │
        ▼
Email genera: jgonzalez@empresa.com
        │
        ▼
Espera aprobacion manual
        │
        ├─────────────────┬─────────────────┐
        │                 │                 │
        ▼                 ▼                 ▼
    [Aprobar]         [Denegar]         [Timeout]
        │                 │                 │
        ▼                 ▼                 ▼
   Continua a        Compensa          Compensa
   Sistemas             LDAP               LDAP
```
