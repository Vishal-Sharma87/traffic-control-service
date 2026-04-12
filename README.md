# 🚦 Traffic Control Service

An asynchronous job processing system built with **Spring Boot**, designed for **high-throughput, fault-tolerant, priority-aware job execution**.

This system simulates a production-grade backend infrastructure component with **retry guarantees, heartbeat monitoring, tier-based prioritization, recovery mechanisms, and Redis resilience**.

---

## 🎯 Objective

Build a scalable async processing layer that:

* Accepts jobs in a **non-blocking** way (HTTP 202)
* Processes jobs using **background worker threads**
* Ensures **at-least-once execution** via retries
* Supports **priority-based scheduling (PAID > UNPAID > PUBLIC)**
* Detects failures using **heartbeat + timeout mechanisms**
* Stores permanently failed jobs in a **Dead Letter Queue (DLQ)**
* **Gates job acceptance** based on downstream system health
* **Self-heals** from Redis outages via exponential backoff

---

## 🏗️ Architecture Overview

```
Client → Controller → RequestService → Queue → Worker → Result Store
                            ↓
                   SystemHealthService
                            ↓
                      Metadata Store
                            ↓
                     Processing Store
                            ↓
                     Recovery Scheduler
                            ↓
                          DLQ
```

---

## ⚙️ Core Features

### 1️⃣ Non-Blocking Job Submission

* Returns `HTTP 202 + jobId`
* Job pushed to **PriorityBlockingQueue**
* Client polls via jobId
* **System health checked before acceptance** — returns 503 if unhealthy

---

### 2️⃣ Priority-Based Scheduling

* Queue: `PriorityBlockingQueue` (min-heap, ordered by score)
* Jobs assigned a **score**:

```java
score = jobTier.getPriority() * PRIORITY_BASE + arrivedAt
```

* Priority order:

| Tier   | Priority |
| ------ | -------- |
| PAID   | Highest  |
| UNPAID | Medium   |
| PUBLIC | Lowest   |

* Retries **preserve original arrival time** → no unfair boost

---

### 3️⃣ Worker System

* `ExecutorService` fixed thread pool
* Workers use `queue.take()` (blocking)
* Infinite loop (interrupt-aware)

---

### 4️⃣ Heartbeat System

* Runs every **10ms**
* Updates `lastHeartBeatTime`
* Detects worker crashes or stalls

---

### 5️⃣ Retry & Recovery System

Scheduler runs periodically with **dynamic delay** (see Feature 8):

Triggers retry when:

* Heartbeat timeout exceeded
* Max processing time exceeded

Retry flow:

```
incrementRetry → status=PENDING → enqueue → remove from processing
```

Discard flow:

```
status=FAILED → DLQ → cleanup
```

---

### 6️⃣ Tier-Based Retry Configuration

| Tier   | maxRetries | heartbeatTimeout | maxProcessingTime |
| ------ | ---------- | ---------------- | ----------------- |
| PUBLIC | 1          | 30ms             | 100ms             |
| UNPAID | 2          | 60ms             | 200ms             |
| PAID   | 5          | 150ms            | 500ms             |

---

### 7️⃣ Dead Letter Queue (DLQ)

* Stores permanently failed jobs
* Fields:

  * jobId
  * jobTier
  * retryCount
  * firstTriedAt
  * discardedAt
  * failureCause

Failure causes:

* `MAX_TIME_EXCEEDED`
* `HEARTBEAT_STOPPED`

---

### 8️⃣ Queue Capacity Strategy

* **Semaphore = soft cap (new jobs)**
* **Retries bypass cap** (guaranteed execution)
* **Health gate bypassed for retries** — system contract must be honored

---

### 9️⃣ System Health Gate & Exponential Backoff (NEW)

`SystemHealthService` tracks downstream (Redis) health and drives two independent concerns:

| Concern | Field | Formula | Purpose |
| --- | --- | --- | --- |
| Acceptance gate | `netSystemFailCount` | `++` fail, `--` success (capped) | reject new jobs when system unreliable |
| Scheduler backoff | `currentDelay` | `*2` fail (ceiling), `/2` success (floor) | reduce load during outage |

**Gate behavior:**
* `netSystemFailCount < threshold` → accept new jobs
* `netSystemFailCount >= threshold` → reject with HTTP 503
* `netSystemFailCeiling` caps fail counter — prevents artificially delayed recovery

**Backoff behavior:**
* Floor: `750ms` (baseline)
* Ceiling: `30000ms` (prevents runaway)
* Exponential growth on failure, exponential decay on recovery
* Scheduler self-heals — no external watchdog needed

**Dynamic scheduler** — replaces `@Scheduled(fixedDelay)`:
```java
// delay read fresh from SystemHealthService after every cycle
ctx -> Instant.now().plusMillis(systemHealthService.getCurrentDelay())
```

**Scheduler records outcome every cycle:**
```java
try {
    // recovery logic
    systemHealthService.recordSuccess();  // currentDelay /= 2
} catch (Exception e) {
    systemHealthService.recordFailure();  // currentDelay *= 2
}
```

---

## 🗂️ Key Components

| Component            | Responsibility                          |
| -------------------- | --------------------------------------- |
| Controller Layer     | Tier-based job submission               |
| RequestService       | Job submission + health gate check      |
| QueueService         | Priority queue + capacity control       |
| JobMetadataService   | Source of truth for job state           |
| Worker Service       | Executes jobs                           |
| Heartbeat Service    | Liveness tracking                       |
| Processing Store     | Active job tracking                     |
| Recovery Scheduler   | Retry / discard decisions               |
| DLQ Service          | Permanent failure storage               |
| SystemHealthService  | Health gate + exponential backoff state |
| JobRecoveryConfig    | Dynamic scheduler wiring via Trigger    |

---

## 📦 Package Structure

```
controllers/
services/
models/
dtos/
entity/
advices/
enums/
configuration/
```

---

## 🧠 Key Design Principles

* **At-least-once execution**
* **Metadata = source of truth**
* **Processing store = transient**
* **Priority is computed, not passed**
* **Retry preserves fairness (no reordering abuse)**
* **Service-level encapsulation of state**
* **computeIfPresent() for atomic updates**
* **YAGNI (no premature abstraction)**
* **Acceptance gate and backoff are separate concerns — one counter cannot serve both**
* **Retried jobs bypass health gate — system contract to user must be honored**
* **Scheduler self-heals Redis recovery — no external watchdog needed**
* **Boundary race on health gate acceptable — slipped job hits error, gets rejected, no corruption**

---

## 🚀 API Endpoints

### Submit Job

```
POST /paid/submit
POST /unpaid/submit
POST /public/submit
```

Response (success):
```json
{ "jobId": "abc-123" }
```

Response (system unhealthy — 503):
```json
{
  "errorCode": "SYSTEM_UNHEALTHY",
  "message": "System is currently unavailable due to high load. Please try again later.",
  "timestamp": "2026-04-12T15:07:39.535765600Z"
}
```

---

### Poll Result

```
GET /result/poll?jobId=
```

---

## ⚙️ Configuration (Simplified)

```yaml
spring:
  profiles:
    active: local  # switch to prod for production environment

traffic-control:

  queue:
    main:
      capacity: "${MAIN_QUEUE_CAPACITY}"
      error:
        queue-full: "${MAIN_QUEUE_FULL_ERROR_MESSAGE}"

  metadata-status:
    response:
      error:
        expired-or-not-exists: "${JOB_EXPIRED_OR_NOT_EXISTS_ERROR_MESSAGE}"
      success:
        completed:  "${JOB_COMPLETED_MESSAGE}"
        pending:    "${JOB_PENDING_MESSAGE}"
        processing: "${JOB_PROCESSING_MESSAGE}"
        failed:     "${JOB_FAILED_MESSAGE}"

  job:
    tier:
      paid:                                           # 5x public, 2.5x unpaid
        max-processing-time: "${PAID_MAX_PROCESSING_TIME}"
        heartbeat-timeout:   "${PAID_MAX_HEARTBEAT_TIMEOUT}"
        max-retries:         "${PAID_MAX_RETRIES_PER_JOB}"
      unpaid:                                         # 2x public
        max-processing-time: "${UNPAID_MAX_PROCESSING_TIME}"
        heartbeat-timeout:   "${UNPAID_MAX_HEARTBEAT_TIMEOUT}"
        max-retries:         "${UNPAID_MAX_RETRIES_PER_JOB}"
      public:                                         # baseline
        max-processing-time: "${PUBLIC_MAX_PROCESSING_TIME}"
        heartbeat-timeout:   "${PUBLIC_MAX_HEARTBEAT_TIMEOUT}"
        max-retries:         "${PUBLIC_MAX_RETRIES_PER_JOB}"

  scheduler:
    delay-floor:   "${SCHEDULER_FLOOR_MS:750}"        # 0.75s — baseline healthy delay
    delay-ceiling: "${SCHEDULER_CEILING_MS:30000}"    # 30s — max backoff during outage

  system:
    threshold:    "${SYSTEM_THRESHOLD:3}"             # gate closes when fail count >= threshold
    fail-ceiling: "${SYSTEM_FAIL_CAP:5}"              # caps fail counter to bound recovery time
    response:
      error:
        unhealthy: "${SYSTEM_UNHEALTHY_ERROR_MESSAGE}"

threads:
  count:
    job-worker-count: "${JOB_WORKERS_COUNT}"
    heartbeat-count:  "${HEARTBEAT_WORKER_COUNT}"     # must equal job-worker-count
  heartbeat:
    initial-delay: "${HEARTBEAT_INITIAL_DELAY}"
    interval:      "${HEARTBEAT_INTERVAL}"
```

---

## ⚠️ Dev Note on Placeholders

Default values have been removed. The application will fail to start if environment variables are not explicitly defined in your local profile. This ensures correct configuration per deployment environment.

---

## 🛣️ Roadmap

* [x] Async processing system
* [x] Retry + recovery scheduler
* [x] Heartbeat system
* [x] DLQ
* [x] Priority queue system
* [x] System health gate + exponential backoff
* [x] Dynamic scheduler (Spring Trigger + SchedulingConfigurer)
* [ ] Redis integration (distributed scheduling)
* [ ] Lua scripts for atomic Redis operations
* [ ] Rate limiting integration
* [ ] Observability (metrics + tracing)

---

## 🧱 Tech Stack

* Java 21
* Spring Boot
* Maven
* Redis (planned)

---

## 👨‍💻 Author

Vishal Sharma

---

## 📄 License

For learning and demonstration purposes.