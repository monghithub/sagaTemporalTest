# Service Equipamiento

## Descripcion

Servicio mock que simula la asignacion de equipos y recursos fisicos al empleado. Es el **cuarto y ultimo paso** del proceso de onboarding.

## Puerto

| Entorno | Puerto |
|---------|--------|
| Desarrollo | 8084 |
| Docker | 10084 |

## Estructura del Proyecto

```
service-equipamiento/
├── src/main/java/com/poc/onboarding/equip/
│   ├── ServiceEquipamientoApplication.java
│   ├── controller/
│   │   └── EquipamientoMockController.java
│   ├── entity/
│   │   └── PeticionEquipamiento.java
│   ├── listener/
│   │   └── EquipamientoRequestListener.java
│   ├── repository/
│   │   └── PeticionEquipamientoRepository.java
│   └── service/
│       └── PeticionService.java
└── src/main/resources/
    ├── application.yml
    └── templates/
        └── peticiones.html
```

## Entidad Principal

### PeticionEquipamiento

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

- **Cola**: `queue.equip.request`
- **Routing Key**: `equip.request`

### Cola de Compensaciones

- **Cola**: `queue.equip.compensate`
- **Routing Key**: `equip.compensate`

Cuando llega una compensacion:
1. Busca la peticion original por `workflowId`
2. **ELIMINA el registro de la base de datos**
3. Envia respuesta de compensacion completada
4. No requiere intervencion manual (automatico)

> **Nota:** En la practica, Equipamiento rara vez recibe compensaciones ya que es el ultimo paso. Solo ocurriria si se implementara un rollback manual.

## Equipamiento Simulado

Este servicio simula asignar:
- Laptop/PC de trabajo
- Monitor(es)
- Teclado y raton
- Telefono corporativo
- Tarjeta de acceso al edificio
- Espacio de trabajo/escritorio

## Configuracion

```yaml
server:
  port: 8084
  forward-headers-strategy: framework
  servlet:
    session:
      tracking-modes: cookie

spring:
  datasource:
    url: jdbc:mysql://localhost:10306/onboarding_equip
  jpa:
    hibernate:
      ddl-auto: update
  rabbitmq:
    host: localhost
    port: 10672

mock-service:
  name: Equipamiento
  icon: bi-laptop
```

## Base de Datos

- **Schema**: `onboarding_equip`
- **Tabla**: `peticion_equipamiento`

## Ultimo Paso del Workflow

Como ultimo paso, tiene caracteristicas especiales:

### Si se Aprueba

```
Equipamiento APROBADO
        │
        ▼
Workflow completa exitosamente
        │
        ▼
Estado final: COMPLETADO
        │
        ▼
Empleado tiene:
├── Usuario LDAP creado
├── Email corporativo activo
├── Accesos a sistemas configurados
└── Equipamiento asignado
```

### Si se Deniega

```
Equipamiento DENEGADO
        │
        ▼
Inicia compensacion de TODOS los pasos anteriores
        │
        ├── Compensar Sistemas
        ├── Compensar Email
        └── Compensar LDAP
        │
        ▼
Estado final: ROLLBACK
        │
        ▼
Todo el onboarding se revierte
```

## Impacto de Denegacion

La denegacion en este punto tiene el mayor impacto porque:
1. Ya se crearon usuario LDAP, email y accesos a sistemas
2. Todas esas operaciones deben revertirse
3. Es la compensacion mas larga del flujo

### Diagrama de Compensacion Completa

```
Equipamiento DENEGADO (registro queda con estado=DENEGADA)
        │
        ▼
┌───────────────────────────────────────────────┐
│ Compensar Sistemas                            │
│ → DELETE de peticion_sistemas                 │
│ → Registro ELIMINADO de BD                    │
└───────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────┐
│ Compensar Email                               │
│ → DELETE de peticion_email                    │
│ → Registro ELIMINADO de BD                    │
└───────────────────────────────────────────────┘
        │
        ▼
┌───────────────────────────────────────────────┐
│ Compensar LDAP                                │
│ → DELETE de peticion_ldap                     │
│ → Registro ELIMINADO de BD                    │
└───────────────────────────────────────────────┘
        │
        ▼
Workflow termina con estado ROLLBACK
```

### Estado de las BDs tras Rollback

| Servicio | Estado de la BD |
|----------|-----------------|
| LDAP | Sin registro (eliminado por compensacion) |
| Email | Sin registro (eliminado por compensacion) |
| Sistemas | Sin registro (eliminado por compensacion) |
| Equipamiento | Registro con estado=DENEGADA (causo el rollback) |
| Central | Proceso con estado=ROLLBACK |

## Consideraciones de Negocio

En un escenario real, la denegacion de equipamiento podria deberse a:
- Presupuesto insuficiente
- Equipos no disponibles en inventario
- Cambio de prioridades
- Empleado decide no unirse

Por eso es importante que el patron Saga permita revertir todos los pasos anteriores de forma coordinada.
