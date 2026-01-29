# DevLog - PoC Saga Pattern con Temporal.io

## Fase 1: Infraestructura Base

### 1.1 Estructura de Directorios
**Fecha**: 2026-01-29

Creando estructura de monorepo Maven:
- `common/` - Módulo compartido (DTOs, eventos, config RabbitMQ)
- `app-central/` - Aplicación principal con worker Temporal
- `service-ldap/` - Mock LDAP/AD
- `service-email/` - Mock Email
- `service-sistemas/` - Mock Sistemas internos
- `service-equipamiento/` - Mock Equipamiento IT

**Archivos creados:**
- `pom.xml` - POM padre con 6 módulos
- `common/pom.xml` - Dependencias: Spring AMQP, Jackson
- `app-central/pom.xml` - Dependencias: Web, JPA, Temporal SDK, Thymeleaf
- `service-*/pom.xml` - Dependencias: Web, JPA, AMQP, Thymeleaf

### 1.2 Docker Compose Infraestructura
**Fecha**: 2026-01-29

Creado `docker-compose.infra.yml` con:
- **MySQL 8.0** (puerto 3306) - Base de datos
- **RabbitMQ 3** (puertos 5672, 15672) - Mensajería
- **Temporal Server 1.22.4** (puerto 7233) - Orquestador
- **Temporal UI 2.22.3** (puerto 8088) - Interfaz web

### 1.3 Inicialización de Bases de Datos
**Fecha**: 2026-01-29

Creado `init-db.sql` con 5 bases de datos:
- `onboarding_central` - App Central (proceso_onboarding, paso_proceso)
- `onboarding_ldap` - Service LDAP (peticion_ldap)
- `onboarding_email` - Service Email (peticion_email)
- `onboarding_sistemas` - Service Sistemas (peticion_sistemas)
- `onboarding_equip` - Service Equipamiento (peticion_equipamiento)

### 1.4 Verificación de Infraestructura
**Fecha**: 2026-01-29

**Conflicto resuelto**: Puertos 3306 y 5672 ya estaban en uso por proyecto CQRS.
Solución: Cambiar a puertos alternativos (3307, 5673).

**Verificación exitosa:**
- MySQL: `localhost:3307` - OK
- RabbitMQ AMQP: `localhost:5673` - OK
- RabbitMQ Management: `localhost:15673` - OK
- Temporal Server: `localhost:7233` - OK
- Temporal UI: `localhost:8088` - OK (HTTP 200)

**Bases de datos creadas:**
```
onboarding_central  (tablas: proceso_onboarding, paso_proceso)
onboarding_ldap     (tablas: peticion_ldap)
onboarding_email    (tablas: peticion_email)
onboarding_sistemas (tablas: peticion_sistemas)
onboarding_equip    (tablas: peticion_equipamiento)
```

---

## Fase 2: Módulo Common

### 2.1 DTOs y Enums
**Fecha**: 2026-01-29

Creados en `common/src/main/java/com/poc/onboarding/common/dto/`:
- `EstadoPeticion.java` - PENDIENTE, APROBADA, DENEGADA, COMPENSADA
- `EstadoProceso.java` - INICIADO, EN_PROGRESO, COMPLETADO, ROLLBACK, ERROR
- `TipoPaso.java` - LDAP, EMAIL, SISTEMAS, EQUIPAMIENTO
- `TipoOperacion.java` - CREAR, ELIMINAR, ASIGNAR, REVOCAR
- `EmpleadoDTO.java` - Datos del empleado
- `PeticionDTO.java` - Datos de una petición

### 2.2 Eventos RabbitMQ
**Fecha**: 2026-01-29

Creados en `common/src/main/java/com/poc/onboarding/common/event/`:
- `PeticionCreatedEvent.java` - Evento cuando se crea una petición
- `PeticionResponseEvent.java` - Evento de respuesta (aprobada/denegada)

### 2.3 Configuración RabbitMQ
**Fecha**: 2026-01-29

Creado `RabbitMQConfig.java` con:
- Exchange: `onboarding.exchange` (topic)
- 12 queues: 3 por servicio (requests, responses, compensate)
- 12 bindings con routing keys

### 2.4 SDKMAN
**Fecha**: 2026-01-29

Creado `.sdkmanrc` para gestión de versiones:
- Java: 17.0.17-amzn
- Maven: 3.9.9

**BUILD SUCCESS** - Módulo common compila correctamente.

---

## Fase 3: App Central

### 3.1 Entidades JPA
- `ProcesoOnboarding.java` - Proceso completo con estado y pasos
- `PasoProceso.java` - Paso individual del proceso

### 3.2 Repositorios
- `ProcesoOnboardingRepository.java`
- `PasoProcesoRepository.java`

### 3.3 Configuración Temporal
- `TemporalConfig.java` - Cliente y WorkerFactory
- `TemporalWorkerConfig.java` - Inicialización del Worker

### 3.4 Workflow y Activities
- `OnboardingWorkflow.java` - Interface del workflow
- `OnboardingWorkflowImpl.java` - Implementación con Saga Pattern
- `OnboardingActivities.java` - Interface de activities
- `OnboardingActivitiesImpl.java` - Publicación en RabbitMQ

### 3.5 Clases auxiliares
- `ApprovalResult.java`, `OnboardingResult.java`, `OnboardingState.java`

### 3.6 Controllers
- `OnboardingController.java` - API REST
- `DashboardController.java` - Vistas Thymeleaf

### 3.7 Listener RabbitMQ
- `ApprovalResponseListener.java` - Recibe respuestas y envía signals

### 3.8 Templates Thymeleaf
- `layout.html` - Layout base
- `dashboard.html` - Lista de procesos
- `proceso-detalle.html` - Detalle con timeline

---

## Fase 4: Servicios Mock

### 4.1 Service LDAP (puerto 8081)
Implementación completa:
- `ServiceLdapApplication.java`
- `PeticionLdap.java` - Entidad con generación de username
- `PeticionLdapRepository.java`
- `PeticionService.java` - Lógica de aprobación/denegación
- `LdapRequestListener.java` - Consume de RabbitMQ
- `LdapMockController.java` - Frontend de aprobación
- `peticiones.html` - UI para aprobar/denegar

### 4.2 Service Email (puerto 8082)
Adaptado de LDAP:
- Genera email corporativo en lugar de username
- Usa queues EMAIL

### 4.3 Service Sistemas (puerto 8083)
Adaptado de LDAP:
- Gestiona accesos a sistemas internos
- Usa queues SISTEMAS

### 4.4 Service Equipamiento (puerto 8084)
Adaptado de LDAP:
- Gestiona asignación de equipamiento IT
- Usa queues EQUIP

**BUILD SUCCESS** - Todos los módulos compilan correctamente.

---

## Fase 1 Completada

**Comando para levantar infraestructura:**
```bash
docker compose -f docker-compose.infra.yml up -d
```

**Accesos:**
| Servicio | URL/Puerto |
|----------|------------|
| MySQL | localhost:3307 (root/root) |
| RabbitMQ AMQP | localhost:5673 (guest/guest) |
| RabbitMQ Management | http://localhost:15673 |
| Temporal gRPC | localhost:7233 |
| Temporal UI | http://localhost:8088 |

---

