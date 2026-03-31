# 🚦 Traffic Control Service

An asynchronous job processing system built with **Spring Boot**, designed to handle job submission, queuing, background processing, retry logic, and result retrieval with high reliability.

Built as an infrastructure-level backend component suitable for high-throughput microservice environments.

---

## 🎯 Objective

To build a scalable and extensible async job processing layer that:

- Accepts jobs from clients and returns immediately (non-blocking)
- Processes jobs in the background using a configurable worker pool
- Retries stuck or failed jobs automatically via a recovery scheduler
- Tracks job lifecycle from submission to completion or permanent failure
- Provides observability through a Dead Letter Queue (DLQ) for permanently failed jobs

---

## 🏗️ Architecture Overview

```
Client Request
      ↓
PublicControllerService  ←→  JobMetadataService (status tracking)
      ↓
   QueueService          ←  Semaphore (soft cap) + ArrayBlockingQueue
      ↓
JobProcessingWorkersService  (ExecutorService fixed thread pool)
      ↓
  Worker Thread
      ↓
 ┌─────────────────────────────────────────┐
 │  CurrentProcessingJobService            │  ← ProcessingInfo tracking
 │  WorkerHeartBeatService                 │  ← 10ms heartbeat per job
 │  ResultService                          │  ← result storage
 │  JobMetadataService                     │  ← status updates
 └─────────────────────────────────────────┘
      ↓ (on failure / timeout)
StuckJobRecoveryService  (@Scheduled recovery)
      ↓
  retry → QueueService
  discard → DlqService
```

---

## ⚙️ Core Features

### 1️⃣ Non-Blocking Job Submission

- Client submits a job and receives HTTP 202 + jobId immediately
- Job pushed to an in-memory priority queue
- Client polls for result using jobId

### 2️⃣ Configurable Worker Pool

- Fixed thread pool via `ExecutorService`
- Workers block on `queue.take()` — no polling or sleep loops
- Worker count configurable via `application.yml`

### 3️⃣ Heartbeat System

- Each active job sends a heartbeat every 10ms via `ScheduledExecutorService`
- Heartbeat updates `lastHeartBeatTime` in the processing store
- Recovery scheduler detects missed heartbeats and triggers retry

### 4️⃣ Automatic Retry & Recovery

- `@Scheduled` recovery runs every 750ms
- Detects stuck jobs via: heartbeat timeout OR max processing time exceeded
- Retries job if `retryCount < maxRetries`, else permanently fails it
- Correct operation order: `incrementRetryCount → PENDING → enqueue → remove from processing store`

### 5️⃣ Dead Letter Queue (DLQ)

- Permanently failed jobs written to DLQ (write-only from system)
- Each entry captures: `jobId`, tier, `retryCount`, `firstTriedAt`, `discardedAt`, `failureCause`
- `failureCause` is an enum: `MAX_TIME_EXCEEDED` or `HEARTBEAT_STOPPED`
- In-memory storage now; DB migration planned

### 6️⃣ Queue Capacity Management

- `Semaphore` enforces soft cap for new jobs atomically
- `ArrayBlockingQueue` capacity = `softCap + workerCount` (provable upper bound)
- Retried jobs bypass semaphore — system has committed to user via jobId, must honor contract

---

## 🗂️ Key Classes

| Class | Purpose |
|---|---|
| `PublicControllerService` | Job submission and result polling |
| `QueueService` | Semaphore + ArrayBlockingQueue wrapper |
| `JobMetadataService` | ConcurrentHashMap jobId → JobMetadata |
| `ResultService` | ConcurrentHashMap jobId → result |
| `JobProcessingWorkersService` | ApplicationRunner + ExecutorService |
| `WorkerHeartBeatService` | ScheduledExecutorService, 10ms heartbeat |
| `CurrentProcessingJobService` | ConcurrentHashMap-backed processing store |
| `StuckJobRecoveryService` | @Scheduled recovery and retry/discard logic |
| `DlqService` | Write-only Dead Letter Queue |

---

## 📦 Package Structure

```
controllers/     → API layer
services/        → business logic
models/          → internal domain objects (NOT DTOs)
dtos/            → API boundary objects only (request/response)
entity/          → JPA/DB entities (future)
advices/         → exception handling, global errors
enums/           → JobStatus, FailureCause, JobTier
```

---

## ⏱️ Timing Configuration

| Parameter | Value | Rationale |
|---|---|---|
| Heartbeat interval | 10ms | 1/3 of avg job time (40ms) |
| Heartbeat timeout | 30ms | 3x heartbeat interval, survives GC pauses |
| Max processing time | 100ms | 2.5x avg job time |
| Scheduler interval | 750ms | 7.5x max processing time |

---

## 📌 Configuration

```yaml
traffic-control:
  queue:
    main:
      capacity: "${MAIN_QUEUE_CAPACITY:5}"

  job:
    max-processing-time: "${MAX_PROCESSING_TIME:100}"
    heartbeat-timeout: "${MAX_HEARTBEAT_TIMEOUT:30}"
    max-retries: "${MAX_RETRIES_PER_JOB:3}"

  scheduler:
    interval: "${SCHEDULER_INTERVAL:750}"

threads:
  count:
    job-worker-count: "${JOB_WORKERS_COUNT:3}"
    heartbeat-count: "${HEARTBEAT_WORKER_COUNT:3}"
  heartbeat:
    initial-delay: "${HEARTBEAT_INITIAL_DELAY:0}"
    interval: "${HEARTBEAT_INTERVAL:10}"
```

---

## 🧠 Key Design Principles

- **At-least-once execution** — idempotent retries using same jobId
- **Metadata = source of truth** — processing store is transient
- **`volatile` for visibility, `AtomicInteger` for read-modify-write, `final` for immutable**
- **`computeIfPresent()` for atomic check+update on ConcurrentHashMap**
- **Single responsibility per service** — QueueService never calls JobMetadataService
- **Service owns flag/state creation** — callers never set internal flags directly
- **YAGNI** — complexity added only when needed, not in anticipation

---

## 🚀 Getting Started

### Run Application

```
mvn spring-boot:run
```

### Submit a Job

```
POST /public/
```

Response:
```json
{ "jobId": "abc-123" }
```

### Poll for Result

```
GET /public/poll?jobId={jobId}
```

---

## 🛣️ Roadmap

- [x] Async job submission and polling
- [x] Configurable worker thread pool
- [x] Heartbeat system
- [x] Automatic retry and recovery scheduler
- [x] Dead Letter Queue (DLQ)
- [ ] Tier-based priority queue (PAID > UNPAID > PUBLIC)
- [ ] Redis migration for metadata store
- [ ] TTL cleanup on metadata
- [ ] Metrics & observability (Micrometer / Prometheus)

---

## 🧱 Technology Stack

- **Java 21**
- **Spring Boot**
- **Maven**
- **Redis** (planned for metadata store migration)

---

## 👨‍💻 Author

Vishal Sharma

---

## 📄 License

For learning and demonstration purposes.