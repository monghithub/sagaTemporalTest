# Service Sistemas

## Descripcion

Servicio mock que simula la asignacion de accesos a sistemas internos de la empresa. Es el **tercer paso** del proceso de onboarding.

## Puerto

| Entorno | Puerto |
|---------|--------|
| Desarrollo | 8083 |
| Docker | 10083 |

## Estructura del Proyecto

```
service-sistemas/
├── src/main/java/com/poc/onboarding/sistemas/
│   ├── ServiceSistemasApplication.java
│   ├── controller/
│   │   └── SistemasMockController.java
│   ├── entity/
│   │   └── PeticionSistemas.java
│   ├── listener/
│   │   └── SistemasRequestListener.java
│   ├── repository/
│   │   └── PeticionSistemasRepository.java
│   └── service/
│       └── PeticionService.java
└── src/main/resources/
    ├── application.yml
    └── templates/
        └── peticiones.html
```

## Entidad Principal

### PeticionSistemas

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | Long | ID autoincremental |
| peticionId | String | UUID unico de la peticion |
| workflowId | String | ID del workflow de Temporal |
| empleadoNombre | String | Nombre del empleado |
| empleadoEmail | String | Email del empleado |
| usernameGenerado | String | Username (de LDAP) |
| estado | EstadoPeticion | PENDIENTE, APROBADA, DENEGADA |
| tipoOperacion | TipoOperacion | CREAR, ELIMINAR |
| motivoDenegacion | String | Motivo si fue denegada |
| fechaCreacion | LocalDateTime | Cuando se creo |
| fechaProcesamiento | LocalDateTime | Cuando se proceso |
| procesadoPor | String | Quien aprobo/denego |

## Listeners RabbitMQ

### Cola de Peticiones

- **Cola**: `queue.sistemas.request`
- **Routing Key**: `sistemas.request`

### Cola de Compensaciones

- **Cola**: `queue.sistemas.compensate`
- **Routing Key**: `sistemas.compensate`

## Sistemas Simulados

Este servicio simula otorgar acceso a:
- ERP corporativo
- CRM
- Intranet
- Sistema de tickets
- Repositorios de codigo

En una implementacion real, cada sistema podria tener su propia integracion.

## Configuracion

```yaml
server:
  port: 8083
  forward-headers-strategy: framework
  servlet:
    session:
      tracking-modes: cookie

spring:
  datasource:
    url: jdbc:mysql://localhost:10306/onboarding_sistemas
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: localhost
    port: 10672

mock-service:
  name: Accesos a Sistemas
  icon: bi-pc-display
```

## Base de Datos

- **Schema**: `onboarding_sistemas`
- **Tabla**: `peticion_sistemas`

## Dependencia con Pasos Anteriores

```
LDAP (username) ──► Email (email corp) ──► Sistemas
                                              │
                                              ▼
                                   Necesita username y
                                   email para configurar
                                   accesos a sistemas
```

## Impacto de Denegacion

Si este paso es denegado:
1. Se envía Signal de denegación a App Central
2. El workflow inicia compensaciones
3. Se compensan Email y LDAP (en ese orden)
4. El proceso termina con estado ROLLBACK

### Ejemplo de Compensacion

```
Sistemas DENEGADO
        │
        ▼
Compensar Sistemas ──► No se creo nada, no hay que compensar
        │
        ▼
Compensar Email ──► Eliminar cuenta jgonzalez@empresa.com
        │
        ▼
Compensar LDAP ──► Eliminar usuario jgonzalez del AD
        │
        ▼
Workflow: ROLLBACK
```

## Flujo de Aprobacion

```
1. Peticion llega via RabbitMQ
        │
        ▼
2. Se muestra en UI con datos del empleado
        │
        ▼
3. Operador revisa y decide
        │
        ├── Aprobar: Continua a Equipamiento
        │
        └── Denegar: Inicia rollback de Email + LDAP
```
