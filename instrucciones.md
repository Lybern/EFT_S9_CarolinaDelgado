# Instrucciones de Compilación, Ejecución y Pruebas — Banco XYZ (EFT)

Este documento detalla el paso a paso para compilar, levantar y verificar el funcionamiento de cada componente del sistema bancario modernizado.

---

## 1. Prerrequisitos
* **JDK:** Java 17 o Java 21 instalado (`java -version`).
* **Maven:** Se incluye el ejecutable Maven Wrapper (`mvnw` y `mvnw.cmd`), por lo que no es estrictamente necesario tener Maven instalado de forma global.
* **Docker y Docker Compose:** Para la ejecución contenerizada (`docker -v`, `docker compose version`).

---

## 2. Compilación del Proyecto Completo

Desde la raíz del proyecto (`EFT_S9_Carolina_Delgado`):

```bash
# En Windows (PowerShell o CMD)
.\mvnw.cmd clean package -DskipTests

# En Linux / macOS
./mvnw clean package -DskipTests
```

---

## 3. Orden de Inicio Recomendado (Modo Local)

Si ejecutas los servicios de forma individual en tu IDE (Visual Studio Code o IntelliJ), sigue este orden estricto de arranque:

1. **Broker de Mensajería:** Iniciar ActiveMQ mediante Docker:
   ```bash
   docker run -d --name activemq -p 61616:61616 -p 8161:8161 apache/activemq-classic:latest
   ```
2. **Config Server (Puerto 8888):**
   ```bash
   cd config-server
   ..\mvnw.cmd spring-boot:run
   ```
   *Verificar en:* `http://localhost:8888/actuator/health`

3. **Discovery Server (Eureka - Puerto 8761):**
   ```bash
   cd discovery-server
   ..\mvnw.cmd spring-boot:run
   ```
   *Verificar panel Eureka en:* `http://localhost:8761` (Usuario: `eureka`, Clave: `eureka2026`)

4. **Auth Server (Puerto 8080):**
   ```bash
   cd auth-server
   ..\mvnw.cmd spring-boot:run
   ```
   *Verificar claves públicas en:* `http://localhost:8080/.well-known/jwks.json`

5. **Core Service (Puerto 8084):**
   ```bash
   cd core-service
   ..\mvnw.cmd spring-boot:run
   ```

6. **MS Mensajería (Puerto 8085):**
   ```bash
   cd ms-mensajeria
   ..\mvnw.cmd spring-boot:run
   ```

7. **Capa BFF:**
   * `bff-web` (Puerto 8081)
   * `bff-movil` (Puerto 8082)
   * `bff-cajero` (Puerto 8083)

8. **Batch Service (Puerto 8087):**
   ```bash
   cd batch-service
   ..\mvnw.cmd spring-boot:run
   ```

---

## 4. Pruebas de Funcionamiento y Evidencias

### 4.1 Verificación de Procesos Batch (Spring Batch)
El microservicio `batch-service` procesa los archivos CSV de la **Semana 3** ubicados en `src/main/resources/data/`:
* `movimientos_financieros_diarios.csv` (Job de transacciones diarias).
* `intereses_trimestrales.csv` (Job de cálculo de intereses).
* `estados_financieros_anuales.csv` (Job de estados de cuenta anuales).

**Verificación:**
* Revisa en la consola del servicio las líneas con los prefijos:
  * `[CHUNK-PROCESSED]` indicando procesamiento por bloques.
  * `[SKIP-POLICY]` registrando los registros erróneos omitidos intencionalmente (montos negativos, edades fuera de rango, fechas inconsistentes).
  * `[AUDIT-LOG]` confirmando que los datos defectuosos fueron derivados a la tabla de anomalías y no rompieron el proceso global.

---

### 4.2 Verificación de la Capa BFF

#### A. Canal Web (BFF Web - Puerto 8081)
Solicita una vista rica con información consolidada de clientes y cuentas:
```bash
curl -X GET http://localhost:8081/api/web/clientes/101/resumen \
     -H "Authorization: Bearer <TOKEN_JWT>"
```
*Respuesta esperada:* JSON con múltiples tablas (cuentas, tarjetas, saldo total, últimos 20 movimientos).

#### B. Canal Móvil (BFF Móvil - Puerto 8082)
Solicita una respuesta ultraligera orientada a smartphones:
```bash
curl -X GET http://localhost:8082/api/movil/clientes/101/saldo-rapido \
     -H "Authorization: Bearer <TOKEN_JWT>"
```
*Respuesta esperada:* Payload compacto de menos de 1 KB con saldo disponible y últimas 3 operaciones resumidas.

#### C. Canal Cajero Automático (BFF Cajero - Puerto 8083)
Ejecuta una consulta o retiro de efectivo:
```bash
curl -X POST http://localhost:8083/api/cajero/retiro \
     -H "Content-Type: application/json" \
     -d '{"cuentaId": 101, "monto": 50000, "pin": "1234"}'
```
*Respuesta esperada:* Confirmación atómica de entrega de dinero con nuevo saldo remanente.

---

### 4.3 Verificación de Resiliencia y Fallback (Resilience4j)
Para comprobar el funcionamiento del Circuit Breaker ante fallos en `core-service`:
1. Detén temporalmente el contenedor de `core-service`.
2. Realiza una petición desde `bff-movil` o `bff-web`.
3. Observa en la respuesta HTTP un estado `200 OK` degradado con la cabecera `X-Fallback-Active: true` o un mensaje controlado del método de respaldo, demostrando que la caída no afectó al cliente final.
