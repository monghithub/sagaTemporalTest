# App Central

## Descripción

App Central es la aplicación principal del sistema de onboarding. Actúa como:
- **Orquestador**: Gestiona los workflows de Temporal
- **Dashboard**: Proporciona la interfaz de usuario principal
- **Hub de comunicación**: Escucha respuestas de los servicios mock

## Estructura del Proyecto

```
app-central/
├── src/main/java/com/poc/onboarding/central/
│   ├── AppCentralApplication.java      # Punto de entrada
│   ├── config/
│   │   └── TemporalWorkerConfig.java   # Configuración del worker de Temporal
│   ├── controller/
│   │   ├── DashboardController.java    # Endpoints del dashboard (Thymeleaf)
│   │   └── OnboardingController.java   # API REST para onboarding
│   ├── entity/
│   │   ├── ProcesoOnboarding.java      # Entidad principal del proceso
│   │   └── PasoProceso.java            # Entidad de cada paso
│   ├── repository/
│   │   ├── ProcesoOnboardingRepository.java
│   │   └── PasoProcesoRepository.java
│   ├── service/
│   │   └── OnboardingService.java      # Lógica de negocio
│   ├── workflow/
│   │   ├── OnboardingWorkflow.java     # Interface del workflow
│   │   └── OnboardingWorkflowImpl.java # Implementación con Saga
│   ├── activity/
│   │   ├── OnboardingActivities.java   # Interface de activities
│   │   └── impl/
│   │       └── OnboardingActivitiesImpl.java
│   └── listener/
│       └── ApprovalResponseListener.java # Listener de RabbitMQ
└── src/main/resources/
    ├── application.yml
    └── templates/
        ├── layout.html
        ├── dashboard.html
        └── proceso-detalle.html
```

## Workflow de Temporal

### Interface del Workflow

```java
@WorkflowInterface
public interface OnboardingWorkflow {

    @WorkflowMethod
    String ejecutarOnboarding(EmpleadoDTO empleado);

    @SignalMethod
    void aprobarPaso(TipoPaso paso, boolean aprobado, Map<String, Object> datos);

    @QueryMethod
    WorkflowState obtenerEstado();
}
```

### Implementación del Patrón Saga

El workflow implementa el patrón Saga con compensaciones:

```java
@Override
public String ejecutarOnboarding(EmpleadoDTO empleado) {
    List<TipoPaso> pasosCompletados = new ArrayList<>();

    try {
        // Ejecutar cada paso secuencialmente
        for (TipoPaso paso : TipoPaso.values()) {
            ejecutarPaso(paso, empleado);
            pasosCompletados.add(paso);
        }
        return "COMPLETADO";
    } catch (Exception e) {
        // Ejecutar compensaciones en orden inverso
        Collections.reverse(pasosCompletados);
        for (TipoPaso paso : pasosCompletados) {
            compensarPaso(paso, empleado);
        }
        return "ROLLBACK";
    }
}
```

### Signals para Aprobaciones

Cada paso espera un Signal de aprobación:

```java
private void esperarAprobacion(TipoPaso paso) {
    Workflow.await(() -> estadoPasos.containsKey(paso));

    if (!estadoPasos.get(paso)) {
        throw new RuntimeException("Paso " + paso + " denegado");
    }
}
```

## Activities

Las activities ejecutan la lógica de cada paso:

| Activity | Función |
|----------|---------|
| `iniciarPaso` | Crea la petición y la envía a RabbitMQ |
| `completarPaso` | Actualiza el estado tras la aprobación |
| `compensarPaso` | Envía mensaje de compensación |
| `actualizarProceso` | Actualiza el estado en la BD |

## Listener de RabbitMQ

`ApprovalResponseListener` escucha las respuestas de los servicios:

```java
@RabbitListener(queues = {
    RabbitMQConfig.QUEUE_LDAP_RESPONSE,
    RabbitMQConfig.QUEUE_EMAIL_RESPONSE,
    RabbitMQConfig.QUEUE_SISTEMAS_RESPONSE,
    RabbitMQConfig.QUEUE_EQUIP_RESPONSE
})
public void handleResponse(PeticionResponseEvent event) {
    // Enviar Signal al workflow correspondiente
    WorkflowStub workflow = client.newUntypedWorkflowStub(event.getWorkflowId());
    workflow.signal("aprobarPaso", event.getTipoPaso(), event.isAprobado(), event.getDatos());
}
```

## Endpoints

### Dashboard (Thymeleaf)

| Método | Ruta | Descripción |
|--------|------|-------------|
| GET | `/` | Dashboard principal con lista de procesos |
| GET | `/proceso/{id}` | Detalle de un proceso |
| GET | `/nuevo` | Formulario de nuevo onboarding |
| POST | `/nuevo` | Crear nuevo proceso |

### API REST

| Método | Ruta | Descripción |
|--------|------|-------------|
| POST | `/api/onboarding` | Iniciar nuevo onboarding |
| GET | `/api/onboarding/{id}` | Obtener estado |
| POST | `/api/onboarding/{id}/rollback` | Solicitar rollback |

## Configuración

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:10306/onboarding_central
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: localhost
    port: 10672

temporal:
  service:
    address: localhost:10233
  namespace: default
```

## Estados del Proceso

| Estado | Descripción |
|--------|-------------|
| INICIADO | Proceso creado, workflow iniciando |
| EN_PROGRESO | Ejecutando pasos, esperando aprobaciones |
| COMPLETADO | Todos los pasos aprobados |
| ROLLBACK | Denegación detectada, compensaciones ejecutadas |
| ERROR | Error técnico durante la ejecución |
