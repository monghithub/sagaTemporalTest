# Base de Datos

## Conexion a MySQL

### Datos de Conexion

| Parametro | Valor |
|-----------|-------|
| Host | localhost |
| Puerto | 10306 |
| Usuario | root |
| Password | root |

### Conexion via CLI

```bash
mysql -h localhost -P 10306 -u root -proot
```

### Conexion via Docker

```bash
docker exec -it temporal-mysql mysql -u root -proot
```

## Schemas Disponibles

| Schema | Servicio | Descripcion |
|--------|----------|-------------|
| `onboarding_central` | App Central | Procesos de onboarding y pasos |
| `onboarding_ldap` | Service LDAP | Peticiones de creacion de usuarios |
| `onboarding_email` | Service Email | Peticiones de cuentas de correo |
| `onboarding_sistemas` | Service Sistemas | Peticiones de accesos a sistemas |
| `onboarding_equip` | Service Equipamiento | Peticiones de asignacion de equipos |

**Importante:** Cada servicio gestiona su propia persistencia de forma **independiente**. Ver seccion "Ciclo de Vida de los Datos" mas abajo.

## Ciclo de Vida de los Datos

### Persistencia por Servicio

Cada servicio mock tiene su propia base de datos y gestiona el ciclo de vida de sus datos:

```
FLUJO NORMAL (Happy Path)
─────────────────────────
Peticion recibida → INSERT (PENDIENTE) → Aprobacion → UPDATE (APROBADA) → Dato PERSISTE


FLUJO CON ROLLBACK (Compensacion)
─────────────────────────────────
Peticion recibida → INSERT (PENDIENTE) → Aprobacion → UPDATE (APROBADA)
                                                            │
                                    [Paso posterior denegado]
                                                            │
                                                            ▼
                                    Compensacion recibida → UPDATE (compensada=true)
                                                            │
                                                            ▼
                                    Dato PERSISTE con marca de ROLLBACK
```

### Que Pasa en Cada Escenario

| Escenario | LDAP | Email | Sistemas | Equipamiento | Central |
|-----------|------|-------|----------|--------------|---------|
| **Todo aprobado** | APROBADA | APROBADA | APROBADA | APROBADA | COMPLETADO |
| **Denegado en LDAP** | DENEGADA | - | - | - | ROLLBACK |
| **Denegado en Email** | APROBADA+ROLLBACK | DENEGADA | - | - | ROLLBACK |
| **Denegado en Sistemas** | APROBADA+ROLLBACK | APROBADA+ROLLBACK | DENEGADA | - | ROLLBACK |
| **Denegado en Equipamiento** | APROBADA+ROLLBACK | APROBADA+ROLLBACK | APROBADA+ROLLBACK | DENEGADA | ROLLBACK |

> **APROBADA+ROLLBACK** significa que el registro mantiene su estado original pero tiene `compensada=true`, mostrando badge rojo "ROLLBACK" en la UI.

### Verificar Estado tras Rollback

```sql
-- Ver procesos con rollback
SELECT * FROM onboarding_central.proceso_onboarding WHERE estado = 'ROLLBACK';

-- Ver peticiones compensadas (marcadas con ROLLBACK)
SELECT id, empleado_nombre, estado, compensada, fecha_compensacion
FROM onboarding_ldap.peticion_ldap WHERE compensada = true;

SELECT id, empleado_nombre, estado, compensada, fecha_compensacion
FROM onboarding_email.peticion_email WHERE compensada = true;

SELECT id, empleado_nombre, estado, compensada, fecha_compensacion
FROM onboarding_sistemas.peticion_sistemas WHERE compensada = true;

SELECT id, empleado_nombre, estado, compensada, fecha_compensacion
FROM onboarding_equip.peticion_equipamiento WHERE compensada = true;

-- Ver todas las peticiones de un workflow con su estado de compensacion
SELECT 'LDAP' as servicio, estado, compensada FROM onboarding_ldap.peticion_ldap WHERE workflow_id = 'onboarding-xxx'
UNION ALL
SELECT 'EMAIL', estado, compensada FROM onboarding_email.peticion_email WHERE workflow_id = 'onboarding-xxx'
UNION ALL
SELECT 'SISTEMAS', estado, compensada FROM onboarding_sistemas.peticion_sistemas WHERE workflow_id = 'onboarding-xxx'
UNION ALL
SELECT 'EQUIPAMIENTO', estado, compensada FROM onboarding_equip.peticion_equipamiento WHERE workflow_id = 'onboarding-xxx';
```

## Consultas Utiles

### Listar todas las bases de datos

```sql
SHOW DATABASES;
```

### Ver tablas de un schema

```sql
USE onboarding_central;
SHOW TABLES;

USE onboarding_ldap;
SHOW TABLES;

USE onboarding_email;
SHOW TABLES;

USE onboarding_sistemas;
SHOW TABLES;

USE onboarding_equip;
SHOW TABLES;
```

## Estructura de Tablas

### onboarding_central

#### proceso_onboarding

```sql
USE onboarding_central;
DESCRIBE proceso_onboarding;
```

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | BIGINT | ID autoincremental |
| workflow_id | VARCHAR(100) | ID del workflow en Temporal |
| empleado_nombre | VARCHAR(100) | Nombre del empleado |
| empleado_email | VARCHAR(100) | Email del empleado |
| estado | ENUM | INICIADO, EN_PROGRESO, COMPLETADO, ROLLBACK, ERROR |
| fecha_inicio | DATETIME | Cuando se inicio el proceso |
| fecha_fin | DATETIME | Cuando termino el proceso |

#### paso_proceso

```sql
DESCRIBE paso_proceso;
```

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | BIGINT | ID autoincremental |
| proceso_id | BIGINT | FK a proceso_onboarding |
| tipo_paso | ENUM | LDAP, EMAIL, SISTEMAS, EQUIPAMIENTO |
| estado | ENUM | PENDIENTE, EN_PROGRESO, COMPLETADO, FALLIDO, COMPENSADO |
| peticion_id | VARCHAR(100) | UUID de la peticion |
| fecha_inicio | DATETIME | Cuando inicio el paso |
| fecha_fin | DATETIME | Cuando termino el paso |
| datos_resultado | TEXT | JSON con datos del resultado |

### onboarding_ldap

#### peticion_ldap

```sql
USE onboarding_ldap;
DESCRIBE peticion_ldap;
```

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | BIGINT | ID autoincremental |
| peticion_id | VARCHAR(100) | UUID unico de la peticion |
| workflow_id | VARCHAR(100) | ID del workflow de Temporal |
| empleado_nombre | VARCHAR(100) | Nombre del empleado |
| empleado_email | VARCHAR(100) | Email del empleado |
| username_generado | VARCHAR(50) | Username generado (ej: jgonzalez) |
| estado | ENUM | PENDIENTE, APROBADA, DENEGADA |
| tipo_operacion | ENUM | CREAR, ELIMINAR |
| motivo_denegacion | TEXT | Motivo si fue denegada |
| fecha_creacion | DATETIME | Cuando se creo |
| fecha_procesamiento | DATETIME | Cuando se proceso |
| procesado_por | VARCHAR(100) | Quien aprobo/denego |

### onboarding_email

#### peticion_email

```sql
USE onboarding_email;
DESCRIBE peticion_email;
```

| Campo | Tipo | Descripcion |
|-------|------|-------------|
| id | BIGINT | ID autoincremental |
| peticion_id | VARCHAR(100) | UUID unico |
| workflow_id | VARCHAR(100) | ID del workflow |
| empleado_nombre | VARCHAR(100) | Nombre del empleado |
| empleado_email | VARCHAR(100) | Email personal |
| email_corporativo | VARCHAR(100) | Email corporativo generado |
| estado | ENUM | PENDIENTE, APROBADA, DENEGADA |
| tipo_operacion | ENUM | CREAR, ELIMINAR |
| motivo_denegacion | TEXT | Motivo si fue denegada |
| fecha_creacion | DATETIME | Cuando se creo |
| fecha_procesamiento | DATETIME | Cuando se proceso |
| procesado_por | VARCHAR(100) | Quien aprobo/denego |

### onboarding_sistemas

#### peticion_sistemas

```sql
USE onboarding_sistemas;
DESCRIBE peticion_sistemas;
```

Estructura similar a peticion_ldap con campo `username_generado`.

### onboarding_equip

#### peticion_equipamiento

```sql
USE onboarding_equip;
DESCRIBE peticion_equipamiento;
```

Estructura similar a peticion_ldap con campo `username_generado`.

## Consultas de Ejemplo

### Ver todos los procesos de onboarding

```sql
USE onboarding_central;
SELECT id, workflow_id, empleado_nombre, estado, fecha_inicio, fecha_fin
FROM proceso_onboarding
ORDER BY fecha_inicio DESC;
```

### Ver pasos de un proceso

```sql
SELECT pp.tipo_paso, pp.estado, pp.fecha_inicio, pp.fecha_fin
FROM paso_proceso pp
JOIN proceso_onboarding po ON pp.proceso_id = po.id
WHERE po.id = 1
ORDER BY pp.tipo_paso;
```

### Ver peticiones pendientes en todos los servicios

```sql
-- LDAP
SELECT 'LDAP' as servicio, empleado_nombre, fecha_creacion
FROM onboarding_ldap.peticion_ldap WHERE estado = 'PENDIENTE'
UNION ALL
-- Email
SELECT 'EMAIL', empleado_nombre, fecha_creacion
FROM onboarding_email.peticion_email WHERE estado = 'PENDIENTE'
UNION ALL
-- Sistemas
SELECT 'SISTEMAS', empleado_nombre, fecha_creacion
FROM onboarding_sistemas.peticion_sistemas WHERE estado = 'PENDIENTE'
UNION ALL
-- Equipamiento
SELECT 'EQUIPAMIENTO', empleado_nombre, fecha_creacion
FROM onboarding_equip.peticion_equipamiento WHERE estado = 'PENDIENTE';
```

### Ver historial de aprobaciones/denegaciones

```sql
USE onboarding_ldap;
SELECT empleado_nombre, estado, procesado_por, fecha_procesamiento, motivo_denegacion
FROM peticion_ldap
WHERE estado != 'PENDIENTE'
ORDER BY fecha_procesamiento DESC;
```

### Contar peticiones por estado

```sql
USE onboarding_ldap;
SELECT estado, COUNT(*) as total
FROM peticion_ldap
GROUP BY estado;
```

## Herramientas de Cliente

### DBeaver (Recomendado)

#### Paso 1: Nueva Conexion

1. Menu: **Database** → **New Database Connection**
2. Seleccionar **MySQL** → Click **Next**

#### Paso 2: Configuracion de Conexion

| Campo | Valor |
|-------|-------|
| Server Host | `localhost` |
| Port | `10306` |
| Database | *(dejar vacio para ver todas)* |
| Username | `root` |
| Password | `root` |

#### Paso 3: Configuracion del Driver

Si es la primera vez, DBeaver pedira descargar el driver de MySQL:
- Click en **Download** cuando aparezca el dialogo

#### Paso 4: Solucionar "Public Key Retrieval is not allowed"

1. En la ventana de conexion, click en la pestana **Driver properties**
2. Busca la propiedad `allowPublicKeyRetrieval`
3. Cambia el valor a `true`
4. (Opcional) Tambien puedes poner `useSSL` en `false` si da problemas de SSL

| Propiedad | Valor |
|-----------|-------|
| allowPublicKeyRetrieval | `true` |
| useSSL | `false` |

#### Paso 5: Probar Conexion

1. Click en **Test Connection...**
2. Deberia mostrar: "Connected"
3. Click **Finish**

#### Navegacion en DBeaver

Una vez conectado, en el panel izquierdo veras:

```
MySQL - localhost:10306
├── Databases
│   ├── onboarding_central
│   │   └── Tables
│   │       ├── paso_proceso
│   │       └── proceso_onboarding
│   ├── onboarding_ldap
│   │   └── Tables
│   │       └── peticion_ldap
│   ├── onboarding_email
│   │   └── Tables
│   │       └── peticion_email
│   ├── onboarding_sistemas
│   │   └── Tables
│   │       └── peticion_sistemas
│   └── onboarding_equip
│       └── Tables
│           └── peticion_equipamiento
```

#### Ver Datos de una Tabla

1. Expande el schema (ej: `onboarding_central`)
2. Expande `Tables`
3. Doble-click en la tabla (ej: `proceso_onboarding`)
4. Se abre pestana con los datos

#### Ejecutar SQL en DBeaver

1. Click derecho en la conexion → **SQL Editor** → **Open SQL Script**
2. Escribe tu consulta
3. **Ctrl+Enter** para ejecutar

#### Consultas Rapidas para Copiar

```sql
-- Ver todos los procesos
SELECT * FROM onboarding_central.proceso_onboarding ORDER BY fecha_inicio DESC;

-- Ver pasos de un proceso
SELECT * FROM onboarding_central.paso_proceso WHERE proceso_id = 1;

-- Ver peticiones LDAP pendientes
SELECT * FROM onboarding_ldap.peticion_ldap WHERE estado = 'PENDIENTE';

-- Ver todas las peticiones de un workflow
SELECT 'LDAP' as servicio, estado, fecha_creacion FROM onboarding_ldap.peticion_ldap WHERE workflow_id = 'onboarding-xxx'
UNION ALL
SELECT 'EMAIL', estado, fecha_creacion FROM onboarding_email.peticion_email WHERE workflow_id = 'onboarding-xxx'
UNION ALL
SELECT 'SISTEMAS', estado, fecha_creacion FROM onboarding_sistemas.peticion_sistemas WHERE workflow_id = 'onboarding-xxx'
UNION ALL
SELECT 'EQUIPAMIENTO', estado, fecha_creacion FROM onboarding_equip.peticion_equipamiento WHERE workflow_id = 'onboarding-xxx';
```

---

### MySQL Workbench

1. Nueva conexion
2. Connection Method: Standard (TCP/IP)
3. Hostname: `localhost`
4. Port: `10306`
5. Username: `root`
6. Password: `root`

### IntelliJ IDEA / DataGrip

1. Database → New → Data Source → MySQL
2. Host: `localhost`
3. Port: `10306`
4. User: `root`
5. Password: `root`

## Limpiar Datos de Prueba

```sql
-- Limpiar todas las peticiones (cuidado en produccion!)
TRUNCATE onboarding_ldap.peticion_ldap;
TRUNCATE onboarding_email.peticion_email;
TRUNCATE onboarding_sistemas.peticion_sistemas;
TRUNCATE onboarding_equip.peticion_equipamiento;

-- Limpiar procesos centrales
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE onboarding_central.paso_proceso;
TRUNCATE onboarding_central.proceso_onboarding;
SET FOREIGN_KEY_CHECKS = 1;
```
