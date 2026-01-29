# Arquitectura del Sistema

## Visión General

Este proyecto implementa una **Proof of Concept (PoC)** del patrón **Saga** utilizando **Temporal.io** para orquestar un proceso de onboarding de empleados con aprobaciones manuales.

## Diagrama de Arquitectura

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              USUARIO (Browser)                               │
└─────────────────────────────────────────────────────────────────────────────┘
                                       │
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
                    ▼                  ▼                  ▼
            ┌──────────────┐   ┌──────────────┐   ┌──────────────┐
            │  App Central │   │ Mock Services│   │  Temporal UI │
            │   (18080)    │   │(10081-10084) │   │   (10088)    │
            └──────┬───────┘   └──────┬───────┘   └──────────────┘
                   │                  │
                   │                  │
                   ▼                  ▼
            ┌─────────────────────────────────────┐
            │            Temporal Server          │
            │              (10233)                │
            └─────────────────┬───────────────────┘
                              │
                              ▼
            ┌─────────────────────────────────────┐
            │             RabbitMQ                │
            │      (10672 AMQP / 10673 Mgmt)      │
            └─────────────────┬───────────────────┘
                              │
         ┌────────────────────┼────────────────────┐
         │                    │                    │
         ▼                    ▼                    ▼
┌─────────────┐      ┌─────────────┐      ┌─────────────┐
│   MySQL     │      │   MySQL     │      │   MySQL     │
│  (Central)  │      │  (Services) │      │ (Temporal)  │
└─────────────┘      └─────────────┘      └─────────────┘
```

## Componentes

### 1. App Central (Puerto 18080)

Aplicación principal que:
- Gestiona el frontend del dashboard
- Orquesta los workflows de Temporal
- Escucha las respuestas de los servicios mock vía RabbitMQ
- Mantiene el estado de los procesos de onboarding

### 2. Servicios Mock (Puertos 10081-10084)

Simulan sistemas externos que requieren aprobación manual:

| Servicio | Puerto | Función |
|----------|--------|---------|
| LDAP | 10081 | Creación de usuario en Active Directory |
| Email | 10082 | Creación de cuenta de correo corporativo |
| Sistemas | 10083 | Asignación de accesos a sistemas internos |
| Equipamiento | 10084 | Asignación de equipos y recursos |

### 3. Temporal Server (Puerto 10233)

Motor de orquestación de workflows que:
- Gestiona el estado de los workflows
- Maneja reintentos y timeouts
- Proporciona durabilidad y consistencia

### 4. RabbitMQ (Puertos 10672/10673)

Broker de mensajería para comunicación asíncrona:
- **Exchange**: `onboarding.exchange` (tipo direct)
- **Colas por servicio**: request, response, compensate

### 5. MySQL (Puerto 10306)

Base de datos compartida con **schemas independientes por servicio**:

| Schema | Servicio | Tabla Principal |
|--------|----------|-----------------|
| `onboarding_central` | App Central | `proceso_onboarding`, `paso_proceso` |
| `onboarding_ldap` | Service LDAP | `peticion_ldap` |
| `onboarding_email` | Service Email | `peticion_email` |
| `onboarding_sistemas` | Service Sistemas | `peticion_sistemas` |
| `onboarding_equip` | Service Equipamiento | `peticion_equipamiento` |

**Cada servicio gestiona su propia persistencia** de forma independiente, lo que permite:
- Aislamiento de datos entre servicios
- Escalabilidad independiente
- Posibilidad de usar diferentes tecnologias de BD por servicio

## Flujo del Patrón Saga

### Happy Path (Todas las aprobaciones exitosas)

```
1. Usuario inicia onboarding
       │
       ▼
2. Workflow Temporal inicia
       │
       ▼
3. Activity: Crear petición LDAP ──────► RabbitMQ ──────► Service LDAP
       │                                                        │
       │◄─────────── Signal de aprobación ◄─────────────────────┘
       │
       ▼
4. Activity: Crear petición EMAIL ─────► RabbitMQ ──────► Service Email
       │                                                        │
       │◄─────────── Signal de aprobación ◄─────────────────────┘
       │
       ▼
5. Activity: Crear petición SISTEMAS ──► RabbitMQ ──────► Service Sistemas
       │                                                        │
       │◄─────────── Signal de aprobación ◄─────────────────────┘
       │
       ▼
6. Activity: Crear petición EQUIPAMIENTO► RabbitMQ ─────► Service Equipamiento
       │                                                        │
       │◄─────────── Signal de aprobación ◄─────────────────────┘
       │
       ▼
7. Workflow completa exitosamente
```

### Compensación (Denegación en algún paso)

```
1-4. Pasos anteriores completados (datos guardados en BD de cada servicio)
       │
       ▼
5. Activity: Crear petición SISTEMAS
       │
       │◄─────────── Signal de DENEGACIÓN
       │
       ▼
6. Ejecutar compensaciones en orden inverso:
       │
       ├──► Compensar SISTEMAS  ──► RabbitMQ ──► Service Sistemas ──► ELIMINA de BD
       │
       ├──► Compensar EMAIL     ──► RabbitMQ ──► Service Email    ──► ELIMINA de BD
       │
       └──► Compensar LDAP      ──► RabbitMQ ──► Service LDAP     ──► ELIMINA de BD
       │
       ▼
7. Workflow termina con estado ROLLBACK
```

### Detalle del Flujo de Compensación

Cuando un paso es denegado, se ejecuta el siguiente flujo para cada servicio que completó su paso:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         FLUJO DE COMPENSACIÓN                                │
└─────────────────────────────────────────────────────────────────────────────┘

App Central                    RabbitMQ                    Service Mock
     │                            │                             │
     │  PeticionCreatedEvent      │                             │
     │  (tipoOperacion=ELIMINAR)  │                             │
     │───────────────────────────►│                             │
     │                            │  queue.xxx.compensate       │
     │                            │────────────────────────────►│
     │                            │                             │
     │                            │                 ┌───────────┴───────────┐
     │                            │                 │ 1. Buscar petición    │
     │                            │                 │    por workflowId     │
     │                            │                 │ 2. ELIMINAR de BD     │
     │                            │                 │ 3. Preparar respuesta │
     │                            │                 └───────────┬───────────┘
     │                            │                             │
     │                            │  PeticionResponseEvent      │
     │                            │◄────────────────────────────│
     │  Signal aprobarPaso        │                             │
     │◄───────────────────────────│                             │
     │                            │                             │
```

**Resultado de la compensación:**
- Los datos de la petición se **marcan como compensados** (`compensada=true`)
- El registro **permanece en la BD** para auditoría con badge "ROLLBACK" en rojo
- El workflow recibe confirmación de que la compensación se completó
- El proceso continúa con la siguiente compensación (orden inverso)

## Comunicación por Mensajes

### Estructura de Colas RabbitMQ

```
Exchange: onboarding.exchange (direct)

Routing Keys y Colas:
├── ldap.request      → queue.ldap.request
├── ldap.response     → queue.ldap.response
├── ldap.compensate   → queue.ldap.compensate
├── email.request     → queue.email.request
├── email.response    → queue.email.response
├── email.compensate  → queue.email.compensate
├── sistemas.request  → queue.sistemas.request
├── sistemas.response → queue.sistemas.response
├── sistemas.compensate → queue.sistemas.compensate
├── equip.request     → queue.equip.request
├── equip.response    → queue.equip.response
└── equip.compensate  → queue.equip.compensate
```

### Eventos

**PeticionCreatedEvent** (Request):
```json
{
  "peticionId": "uuid",
  "workflowId": "onboarding-xxx",
  "tipoPaso": "LDAP|EMAIL|SISTEMAS|EQUIPAMIENTO",
  "tipoOperacion": "CREAR|ELIMINAR",
  "empleadoNombre": "string",
  "empleadoEmail": "string",
  "datos": {}
}
```

**PeticionResponseEvent** (Response):
```json
{
  "peticionId": "uuid",
  "workflowId": "onboarding-xxx",
  "tipoPaso": "LDAP|EMAIL|SISTEMAS|EQUIPAMIENTO",
  "aprobado": true|false,
  "datos": {},
  "motivoRechazo": "string",
  "procesadoPor": "string"
}
```

## Stack Tecnológico

| Componente | Tecnología | Versión |
|------------|------------|---------|
| Backend | Java | 17 |
| Framework | Spring Boot | 3.2.5 |
| Orquestación | Temporal SDK | 1.22.x |
| Mensajería | Spring AMQP | 3.x |
| Persistencia | Spring Data JPA | 3.x |
| Base de datos | MySQL | 8.0 |
| Frontend | Thymeleaf + Bootstrap | 5.3.2 |
| Build | Maven | 3.9.9 |
| Contenedores | Docker Compose | 2.x |

## Configuración de Puertos

| Servicio | Puerto | Descripción |
|----------|--------|-------------|
| App Central | 18080 | Dashboard principal |
| Service LDAP | 10081 | Mock LDAP/AD |
| Service Email | 10082 | Mock Email corporativo |
| Service Sistemas | 10083 | Mock accesos a sistemas |
| Service Equipamiento | 10084 | Mock asignación equipos |
| Temporal UI | 10088 | Interfaz de Temporal |
| Temporal Server | 10233 | gRPC API de Temporal |
| RabbitMQ AMQP | 10672 | Protocolo AMQP |
| RabbitMQ Management | 10673 | Interfaz web RabbitMQ |
| MySQL | 10306 | Base de datos |

## Decisiones de Diseño

### 1. Orquestación vs Coreografía
Se eligió **orquestación** con Temporal porque:
- Control centralizado del flujo
- Mejor visibilidad del estado
- Manejo automático de reintentos
- Compensaciones coordinadas

### 2. Comunicación Asíncrona
RabbitMQ para desacoplar los servicios:
- Los servicios mock pueden procesar a su ritmo
- Aprobaciones manuales sin bloquear el workflow
- Temporal usa Signals para recibir respuestas

### 3. Base de datos por servicio
Cada servicio tiene su propio schema:
- Aislamiento de datos
- Independencia de deployment
- Facilita migración a microservicios reales

### 4. Auto-refresh condicional
El frontend hace refresh automático pero:
- Solo si no hay modales abiertos
- Evita interrumpir la experiencia de usuario

### 5. Persistencia y Ciclo de Vida de Datos

Cada servicio mock gestiona su propia persistencia de forma independiente:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    CICLO DE VIDA DE DATOS POR SERVICIO                       │
└─────────────────────────────────────────────────────────────────────────────┘

PETICIÓN RECIBIDA (queue.xxx.request)
        │
        ▼
┌───────────────────┐
│ CREAR registro    │
│ estado=PENDIENTE  │
│ en BD del servicio│
└─────────┬─────────┘
          │
          ▼
    ┌─────────────┐
    │  APROBACIÓN │
    │   MANUAL    │
    └──────┬──────┘
           │
     ┌─────┴─────┐
     │           │
     ▼           ▼
┌─────────┐  ┌─────────┐
│ APROBAR │  │ DENEGAR │
└────┬────┘  └────┬────┘
     │            │
     ▼            ▼
┌─────────┐  ┌─────────────────┐
│ UPDATE  │  │ UPDATE estado   │
│ estado= │  │ =DENEGADA       │
│ APROBADA│  │ Workflow inicia │
└────┬────┘  │ compensaciones  │
     │       └────────┬────────┘
     │                │
     ▼                ▼
┌─────────┐  ┌─────────────────┐
│ Dato    │  │ COMPENSACIÓN    │
│ PERSISTE│  │ recibida        │
│ en BD   │  │ (queue.xxx.     │
└─────────┘  │  compensate)    │
             └────────┬────────┘
                      │
                      ▼
             ┌─────────────────┐
             │ DELETE registro │
             │ de la BD        │
             └─────────────────┘
```

**Resumen del estado de datos:**

| Escenario | Estado en BD del Servicio | UI |
|-----------|---------------------------|-----|
| Petición pendiente | `estado=PENDIENTE` | Badge amarillo |
| Paso aprobado | `estado=APROBADA` | Badge verde |
| Paso denegado | `estado=DENEGADA` | Badge rojo |
| Compensación ejecutada | `estado=APROBADA, compensada=true` | Badge verde + **ROLLBACK** rojo |

> **Nota:** El registro NO se elimina, se marca como `compensada=true` para mantener trazabilidad.

### 6. Consistencia Eventual

El sistema implementa **consistencia eventual** mediante:
- Mensajería asíncrona (RabbitMQ)
- Compensaciones automáticas del patrón Saga
- Marcado de datos como compensados en rollback para mantener trazabilidad

### 7. Contenedores sin Auto-arranque

Los contenedores Docker están configurados con `restart: "no"`:
- **No se inician automáticamente** al arrancar el sistema
- Deben iniciarse manualmente con `docker compose up -d`
- Permite control total sobre cuándo ejecutar el entorno
- Evita consumo de recursos cuando no se está usando
