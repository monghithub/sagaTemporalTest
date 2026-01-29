# Base de Datos

## Conexion a MySQL

### Datos de Conexion

| Parametro | Valor |
|-----------|-------|
| Host | localhost |
| Puerto | 10306 |
| Usuario | root |
| Password | rootpassword |

### Conexion via CLI

```bash
mysql -h localhost -P 10306 -u root -prootpassword
```

### Conexion via Docker

```bash
docker exec -it temporal-mysql mysql -u root -prootpassword
```

## Schemas Disponibles

| Schema | Servicio | Descripcion |
|--------|----------|-------------|
| `onboarding_central` | App Central | Procesos de onboarding y pasos |
| `onboarding_ldap` | Service LDAP | Peticiones de creacion de usuarios |
| `onboarding_email` | Service Email | Peticiones de cuentas de correo |
| `onboarding_sistemas` | Service Sistemas | Peticiones de accesos a sistemas |
| `onboarding_equip` | Service Equipamiento | Peticiones de asignacion de equipos |

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

### DBeaver

1. Nueva conexion → MySQL
2. Host: `localhost`
3. Puerto: `10306`
4. Usuario: `root`
5. Password: `rootpassword`

### MySQL Workbench

1. Nueva conexion
2. Connection Method: Standard (TCP/IP)
3. Hostname: `localhost`
4. Port: `10306`
5. Username: `root`
6. Password: `rootpassword`

### IntelliJ IDEA / DataGrip

1. Database → New → Data Source → MySQL
2. Host: `localhost`
3. Port: `10306`
4. User: `root`
5. Password: `rootpassword`

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
