# Saga Pattern PoC con Temporal.io

Proof of Concept del patron **Saga** utilizando **Temporal.io** para orquestar un proceso de onboarding de empleados con aprobaciones manuales.

## Descripcion

Este proyecto demuestra como implementar transacciones distribuidas con compensaciones automaticas usando el patron Saga. El caso de uso es un proceso de onboarding que requiere aprobacion manual en cada paso.

### Flujo del Proceso

```
┌─────────┐    ┌─────────┐    ┌──────────┐    ┌──────────────┐
│  LDAP   │───►│  EMAIL  │───►│ SISTEMAS │───►│ EQUIPAMIENTO │
│ Crear   │    │ Crear   │    │  Asignar │    │   Asignar    │
│ Usuario │    │ Cuenta  │    │  Accesos │    │   Equipos    │
└─────────┘    └─────────┘    └──────────┘    └──────────────┘
     │              │              │                  │
     ▼              ▼              ▼                  ▼
 Aprobacion    Aprobacion    Aprobacion         Aprobacion
   Manual        Manual        Manual             Manual
```

Si cualquier paso es denegado, se ejecutan compensaciones en orden inverso.

## Documentacion

| Documento | Descripcion |
|-----------|-------------|
| [Arquitectura](docs/ARCHITECTURE.md) | Vision general del sistema, componentes y decisiones de diseno |
| [Base de Datos](docs/DATABASE.md) | Conexion a MySQL, schemas, tablas y consultas utiles |
| [App Central](docs/APP-CENTRAL.md) | Aplicacion principal, workflow Temporal y API |
| [Service LDAP](docs/SERVICE-LDAP.md) | Servicio mock de Active Directory |
| [Service Email](docs/SERVICE-EMAIL.md) | Servicio mock de correo corporativo |
| [Service Sistemas](docs/SERVICE-SISTEMAS.md) | Servicio mock de accesos a sistemas |
| [Service Equipamiento](docs/SERVICE-EQUIPAMIENTO.md) | Servicio mock de asignacion de equipos |

## Stack Tecnologico

- **Java 17**
- **Spring Boot 3.2.5**
- **Temporal SDK 1.22.x**
- **RabbitMQ** (mensajeria asincrona)
- **MySQL 8.0**
- **Thymeleaf + Bootstrap 5**
- **Docker Compose**
- **Maven** (monorepo)

## Inicio Rapido

### Prerequisitos

- Docker y Docker Compose
- Java 17+
- Maven 3.9+

### Levantar Infraestructura

```bash
docker-compose -f docker-compose.infra.yml up -d
```

Esto inicia:
- MySQL (puerto 10306)
- RabbitMQ (puertos 10672/10673)
- Temporal Server (puerto 10233)
- Temporal UI (puerto 10088)

### Compilar y Ejecutar

```bash
# Compilar todo el proyecto
mvn clean install -DskipTests

# Opcion 1: Ejecutar todo con Docker
docker-compose up --build

# Opcion 2: Ejecutar localmente (para desarrollo)
# Terminal 1 - App Central
cd app-central && mvn spring-boot:run

# Terminal 2-5 - Servicios Mock
cd service-ldap && mvn spring-boot:run
cd service-email && mvn spring-boot:run
cd service-sistemas && mvn spring-boot:run
cd service-equipamiento && mvn spring-boot:run
```

## URLs de Acceso

| Servicio | URL | Descripcion |
|----------|-----|-------------|
| App Central | http://localhost:18080 | Dashboard principal |
| Service LDAP | http://localhost:10081 | Aprobaciones LDAP |
| Service Email | http://localhost:10082 | Aprobaciones Email |
| Service Sistemas | http://localhost:10083 | Aprobaciones Sistemas |
| Service Equipamiento | http://localhost:10084 | Aprobaciones Equipamiento |
| Temporal UI | http://localhost:10088 | Interfaz de Temporal |
| RabbitMQ | http://localhost:10673 | Gestion de colas (guest/guest) |

## Flujo de Prueba

### Happy Path (todas las aprobaciones exitosas)

1. Acceder a http://localhost:18080
2. Crear nuevo proceso de onboarding
3. Aprobar en cada servicio mock (LDAP → Email → Sistemas → Equipamiento)
4. Verificar que el proceso termina con estado **COMPLETADO**

### Prueba de Compensacion

1. Crear nuevo proceso de onboarding
2. Aprobar LDAP y Email
3. **Denegar** en Sistemas
4. Verificar que se ejecutan compensaciones para Email y LDAP
5. El proceso termina con estado **ROLLBACK**

## Estructura del Proyecto

```
sagaTemporalTest/
├── pom.xml                    # POM padre
├── docker-compose.yml         # Todos los servicios
├── docker-compose.infra.yml   # Solo infraestructura
├── init-db.sql                # Inicializacion de BDs
├── docs/                      # Documentacion
│   ├── ARCHITECTURE.md
│   ├── DATABASE.md
│   ├── APP-CENTRAL.md
│   ├── SERVICE-LDAP.md
│   ├── SERVICE-EMAIL.md
│   ├── SERVICE-SISTEMAS.md
│   └── SERVICE-EQUIPAMIENTO.md
├── common/                    # Modulo compartido
├── app-central/               # Aplicacion principal
├── service-ldap/              # Mock LDAP
├── service-email/             # Mock Email
├── service-sistemas/          # Mock Sistemas
└── service-equipamiento/      # Mock Equipamiento
```

## Patron Saga con Temporal

### Ventajas de usar Temporal

1. **Durabilidad**: El estado del workflow persiste aunque los servicios se reinicien
2. **Reintentos automaticos**: Manejo configurable de fallos transitorios
3. **Visibilidad**: UI para monitorear workflows en tiempo real
4. **Signals**: Permite recibir eventos externos (aprobaciones)
5. **Compensaciones coordinadas**: Rollback automatico en orden inverso

### Implementacion

```java
@Override
public String ejecutarOnboarding(EmpleadoDTO empleado) {
    List<TipoPaso> pasosCompletados = new ArrayList<>();

    try {
        for (TipoPaso paso : TipoPaso.values()) {
            ejecutarPaso(paso, empleado);
            pasosCompletados.add(paso);
        }
        return "COMPLETADO";
    } catch (Exception e) {
        // Compensar en orden inverso
        Collections.reverse(pasosCompletados);
        for (TipoPaso paso : pasosCompletados) {
            compensarPaso(paso, empleado);
        }
        return "ROLLBACK";
    }
}
```

## Comunicacion por Mensajes

La comunicacion entre App Central y los servicios mock es asincrona via RabbitMQ:

```
App Central ──► RabbitMQ ──► Service Mock
                   │
                   │ (respuesta)
                   │
App Central ◄──────┘
```

### Colas por Servicio

- `queue.ldap.request` / `queue.ldap.response` / `queue.ldap.compensate`
- `queue.email.request` / `queue.email.response` / `queue.email.compensate`
- `queue.sistemas.request` / `queue.sistemas.response` / `queue.sistemas.compensate`
- `queue.equip.request` / `queue.equip.response` / `queue.equip.compensate`

## Contribuir

Este es un proyecto de demostracion. Para mejoras o sugerencias, abrir un issue.

## Licencia

MIT
