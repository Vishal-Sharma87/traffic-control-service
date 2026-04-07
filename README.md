# 🚦 Traffic Control Service

An asynchronous job processing system built with **Spring Boot**, designed for **high-throughput, fault-tolerant, priority-aware job execution**.

This system simulates a production-grade backend infrastructure component with **retry guarantees, heartbeat monitoring, tier-based prioritization, and recovery mechanisms**.

---

## 🎯 Objective

Build a scalable async processing layer that:

* Accepts jobs in a **non-blocking** way (HTTP 202)
* Processes jobs using **background worker threads**
* Ensures **at-least-once execution** via retries
* Supports **priority-based scheduling (PAID > UNPAID > PUBLIC)**
* Detects failures using **heartbeat + timeout mechanisms**
* Stores permanently failed jobs in a **Dead Letter Queue (DLQ)**

---

## 🏗️ Architecture Overview

```
Client → Controller → Queue → Worker → Result Store
                      ↓
                Metadata Store
                      ↓
               Processing Store
                      ↓
                    DLQ
```

---

## ⚙️ Core Features

### 1️⃣ Non-Blocking Job Submission

* Returns `HTTP 202 + jobId`
* Job pushed to **PriorityBlockingQueue**
* Client polls via jobId

---

### 2️⃣ Priority-Based Scheduling (NEW)

* Queue upgraded: `ArrayBlockingQueue` → `PriorityBlockingQueue`
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

Scheduler (`@Scheduled`) runs periodically:

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

### 6️⃣ Tier-Based Retry Configuration (NEW)

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

---

## 🗂️ Key Components

| Component          | Responsibility                    |
| ------------------ | --------------------------------- |
| Controller Layer   | Tier-based job submission         |
| QueueService       | Priority queue + capacity control |
| JobMetadataService | Source of truth for job state     |
| Worker Service     | Executes jobs                     |
| Heartbeat Service  | Liveness tracking                 |
| Processing Store   | Active job tracking               |
| Recovery Scheduler | Retry / discard decisions         |
| DLQ Service        | Permanent failure storage         |

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

---

## 🚀 API Endpoints

### Submit Job

```
POST /paid/submit
POST /unpaid/submit
POST /public/submit
```

Response:

```json
{ "jobId": "abc-123" }
```

---

### Poll Result

```
GET /result/poll?jobId=
```
---

## ⚙️ Configuration (Simplified)

```
traffic-control:
  job:
    tier:
      paid:
        max-processing-time: "${PAID_MAX_PROCESSING_TIME}"
        heartbeat-timeout: "${PAID_MAX_HEARTBEAT_TIMEOUT}"
        max-retries: "${PAID_MAX_RETRIES_PER_JOB}"

      unpaid:
        max-processing-time: "${UNPAID_MAX_PROCESSING_TIME}"
        heartbeat-timeout: "${UNPAID_MAX_HEARTBEAT_TIMEOUT}"
        max-retries: "${UNPAID_MAX_RETRIES_PER_JOB}"

      public:
        max-processing-time: "${PUBLIC_MAX_PROCESSING_TIME}"
        heartbeat-timeout: "${PUBLIC_MAX_HEARTBEAT_TIMEOUT}"
        max-retries: "${PUBLIC_MAX_RETRIES_PER_JOB}"
        
threads:
  count:
    # Workers and Heartbeats must be scaled 1:1
    job-worker-count: "${JOB_WORKERS_COUNT}"
    heartbeat-count: "${HEARTBEAT_WORKER_COUNT}"
```
---

## ⚠️ Dev Note on Placeholders

The default values (e.g., :100) have been removed. The application will now fail to start if the environment variables are not explicitly defined in your local profile or system environment. This ensures that fellow programmers configure the service correctly for their specific use cases.

---

## 🛣️ Roadmap

* [x] Async processing system
* [x] Retry + recovery scheduler
* [x] Heartbeat system
* [x] DLQ
* [x] Priority queue system
* [ ] Redis (distributed scheduling)
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
