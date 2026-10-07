# Banco XYZ — Sistema Modernizado Backend III (EFT)

**Institución:** Duoc UC  
**Asignatura:** Desarrollo Backend III (PBY2203)  
**Evaluación:** Evaluación Final Transversal (EFT)  
**Estudiante:** Carolina Delgado Sapunar  
**Repositorio GitHub:** [https://github.com/Lybern/EFT_S9_CarolinaDelgado](https://github.com/Lybern/EFT_S9_CarolinaDelgado)

---

## 1. Resumen Ejecutivo del Proyecto
El proyecto implementa la modernización integral del sistema bancario del **Banco XYZ**, migrando su infraestructura legacy basada en COBOL, scripts Shell y Mainframe hacia una arquitectura distribuida en la nube con microservicios, patrón Backend for Frontend (BFF), mensajería asíncrona y procesamiento por lotes con Spring Batch.

---

## 2. Estructura de la Solución Multi-Módulo

| Módulo | Tipo | Puerto | Descripción |
| :--- | :--- | :---: | :--- |
| `discovery-server` | Infraestructura | `8761` | Servidor Eureka para descubrimiento y registro dinámico de servicios. |
| `config-server` | Infraestructura | `8888` | Servidor centralizado de configuración con Spring Cloud Config. |
| `auth-server` | Seguridad | `8080` | Servidor de autorización OAuth2 con emisión de tokens JWT RSA. |
| `core-service` | Dominio Core | `8084` | Gestión de cuentas, transferencias, retiros, resiliencia con Resilience4j. |
| `ms-mensajeria` | Eventos | `8085` | Consumidor asíncrono desacoplado para eventos de transacciones. |
| `bff-web` | Capa BFF | `8081` | BFF especializado para navegadores web (payloads consolidados). |
| `bff-movil` | Capa BFF | `8082` | BFF especializado para app móvil (payloads compactos y ligeros). |
| `bff-cajero` | Capa BFF | `8083` | BFF especializado para cajeros automáticos ATM (operaciones atómicas). |
| `batch-service` | Procesamiento Lotes | `8087` | Spring Batch con los 3 Jobs procesando datos legacy de la **Semana 3**. |

---

## 3. Requerimientos Técnicos Cumplidos
1. **5 Procesos Críticos:** Spring Batch, Microservicios, BFF, Seguridad Distribuida y Mensajería Asíncrona.
2. **3 Requerimientos de Negocio:** Optimización por canal (R1), Alta resiliencia transaccional (R2) y Procesamiento masivo tolerante a anomalías (R3).
3. **Spring Batch (Semana 3):** Transacciones diarias, Intereses y Estados anuales con *Chunk*, *SkipPolicy*, *RetryPolicy* y particionado multihilo.
4. **Resiliencia & Eventos:** Circuit Breaker, Rate Limiter, Retry y colas de mensajes JMS/ActiveMQ.
5. **Contenedores:** Dockerfile para cada servicio y orquestación con `docker-compose.yml`.

---

## 4. Guías Rápidas
* Para ver las instrucciones detalladas de compilación y prueba de endpoints, consulta [instrucciones.md](file:///instrucciones.md).
* Para ver los pasos de despliegue en contenedores y la nube AWS, consulta [despliegue.md](file:///despliegue.md).
