# Service LDAP

## Descripcion

Servicio mock que simula la gestion de usuarios en Active Directory / LDAP. Es el **primer paso** del proceso de onboarding.

## Puerto

| Entorno | Puerto |
|---------|--------|
| Desarrollo | 8081 |
| Docker | 10081 |

## Estructura del Proyecto

```
service-ldap/
├── src/main/java/com/poc/onboarding/ldap/
│   ├── ServiceLdapApplication.java
│   ├── controller/
│   │   └── LdapMockController.java
│   ├── entity/
│   │   └── PeticionLdap.java
│   ├── listener/
│   │   └── LdapRequestListener.java
│   ├── repository/
│   │   └── PeticionLdapRepository.java
│   └── service/
│       └── PeticionService.java
└── src/main/resources/
    ├── application.yml
    └── templates/
        └── peticiones.html
```

## Entidad Principal

### PeticionLdap

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | Long | ID autoincremental |
| peticionId | String | UUID unico de la peticion |
| workflowId | String | ID del workflow de Temporal |
| empleadoNombre | String | Nombre del empleado |
| empleadoEmail | String | Email del empleado |
| usernameGenerado | String | Username generado (ej: jgonzalez) |
| estado | EstadoPeticion | PENDIENTE, APROBADA, DENEGADA |
| tipoOperacion | TipoOperacion | CREAR, ELIMINAR |
| motivoDenegacion | String | Motivo si fue denegada |
| fechaCreacion | LocalDateTime | Cuando se creo |
| fechaProcesamiento | LocalDateTime | Cuando se proceso |
| procesadoPor | String | Quien aprobo/denego |

### Generacion de Username

El username se genera automaticamente a partir del email:
- `jose.gonzalez@empresa.com` → `jgonzalez`
- `maria.lopez@empresa.com` → `mlopez`

```java
public void generarUsername() {
    String[] parts = empleadoEmail.split("@")[0].split("\\.");
    this.usernameGenerado = parts[0].charAt(0) + parts[1];
}
```

## Listeners RabbitMQ

### Cola de Peticiones

- **Cola**: `queue.ldap.request`
- **Routing Key**: `ldap.request`

Cuando llega una peticion:
1. Crea registro en BD con estado PENDIENTE
2. Genera username propuesto
3. Espera aprobacion manual en la UI

### Cola de Compensaciones

- **Cola**: `queue.ldap.compensate`
- **Routing Key**: `ldap.compensate`

Cuando llega una compensacion:
1. Se procesa automaticamente (simula eliminacion del usuario)
2. Envia respuesta aprobada inmediatamente
3. No requiere intervencion manual

## Servicio de Peticiones

### Metodos Principales

| Metodo | Descripcion |
|--------|-------------|
| `obtenerPendientes()` | Lista peticiones pendientes de aprobacion |
| `obtenerHistorial()` | Lista peticiones ya procesadas |
| `aprobar(id, procesadoPor)` | Aprueba peticion y notifica via RabbitMQ |
| `denegar(id, motivo, procesadoPor)` | Deniega peticion y notifica via RabbitMQ |

### Flujo de Aprobacion

```
1. Usuario pulsa "Aprobar" en la UI
        │
        ▼
2. PeticionService.aprobar()
        │
        ├── Actualiza estado a APROBADA
        ├── Registra quien aprobo
        └── Envia PeticionResponseEvent a RabbitMQ
                │
                ▼
3. App Central recibe Signal
        │
        ▼
4. Workflow continua al siguiente paso
```

### Flujo de Denegacion

```
1. Usuario pulsa "Denegar" en la UI
        │
        ├── Introduce motivo de denegacion
        │
        ▼
2. PeticionService.denegar()
        │
        ├── Actualiza estado a DENEGADA
        ├── Guarda motivo
        └── Envia PeticionResponseEvent(aprobado=false)
                │
                ▼
3. App Central recibe Signal de denegacion
        │
        ▼
4. Workflow inicia compensaciones
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

### Vista Principal (`peticiones.html`)

La pagina muestra:
- **Peticiones Pendientes**: Cards con informacion del empleado y botones Aprobar/Denegar
- **Historial**: Tabla con peticiones procesadas

### Caracteristicas

- Auto-refresh cada 3 segundos (solo si no hay modal abierto)
- Link dinamico a App Central usando hostname del navegador
- Indicador visual de tipo de operacion (CREAR/ELIMINAR)
- Modal para capturar motivo de denegacion

## Configuracion

```yaml
server:
  port: 8081
  forward-headers-strategy: framework
  servlet:
    session:
      tracking-modes: cookie

spring:
  datasource:
    url: jdbc:mysql://localhost:10306/onboarding_ldap
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: localhost
    port: 10672

mock-service:
  name: LDAP/Active Directory
  icon: bi-person-badge
```

## Base de Datos

- **Schema**: `onboarding_ldap`
- **Tabla**: `peticion_ldap`

Las tablas se crean automaticamente via Hibernate (`ddl-auto: update`).

## Diagrama de Secuencia

```
App Central           RabbitMQ            Service LDAP           Usuario
    │                    │                     │                    │
    │ PeticionCreatedEvent                     │                    │
    │───────────────────►│                     │                    │
    │                    │ queue.ldap.request  │                    │
    │                    │────────────────────►│                    │
    │                    │                     │ Guarda en BD       │
    │                    │                     │────────────────────►
    │                    │                     │                    │
    │                    │                     │◄───────────────────┤
    │                    │                     │ Aprobar/Denegar    │
    │                    │                     │                    │
    │                    │ PeticionResponseEvent                    │
    │                    │◄────────────────────│                    │
    │ Signal aprobarPaso │                     │                    │
    │◄───────────────────│                     │                    │
    │                    │                     │                    │
```
