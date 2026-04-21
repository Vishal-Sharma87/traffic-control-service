# 🚦 Traffic Control Service

An asynchronous, fault-tolerant job processing system built with **Java 21 + Spring Boot + Redis + MySQL**.

Designed to demonstrate production-grade backend engineering — including priority scheduling, distributed state management, heartbeat-based crash detection, automatic recovery, and permanent failure handling via a Dead Letter Queue.

---

> 📐 **Architecture Diagram**
>
> ![System Architecture](docs/System_Architechture.png)

---

## 🎯 What This System Does

A client submits a job and immediately receives a `jobId` (HTTP 202). The job is processed in the background by a pool of worker threads. The client polls for the result using the `jobId`.

Under the hood, the system:
- Prioritizes jobs by tier (PAID > UNPAID > PUBLIC)
- Tracks every active job via heartbeat
- Automatically retries crashed or stuck jobs
- Permanently discards jobs that exhaust retries — logged to a DLQ in MySQL
- Rejects new jobs when the system is under stress

---

## 🏗️ Architecture

```
Client
  └── POST /submit  (202 + jobId)
        └── RequestService
              ├── Health gate check (rejects if system unhealthy)
              ├── Capacity check (rejects if queue full)
              └── Push to Main Queue (Redis ZSET)
                        └── Worker Thread Pool
                              ├── Pop job from queue (ZPOPMIN)
                              ├── Initialise processing store (atomic Lua)
                              ├── Start heartbeat
                              ├── Process job
                              ├── Save result → MySQL
                              └── Complete + cleanup (atomic Lua)

Recovery Layer (runs independently)
  ├── CrashedJobRecoveryService  →  processing:by-heartbeat ZSET
  └── StuckJobRecoveryService    →  processing:by-maxtime ZSET
        └── Lua recovery script
              ├── Within retry limit → requeue
              └── Limit exhausted   → DLQ (MySQL)
```

---

## ⚙️ Core Features

### 1️⃣ Non-Blocking Job Submission
- Returns `HTTP 202 + jobId` immediately
- Job pushed atomically to Redis ZSET (main queue)
- Metadata initialized in Redis Hash
- Client polls via `GET /result/poll?jobId=`

---

### 2️⃣ Priority-Based Scheduling (Redis ZSET)

The main queue is a **Redis Sorted Set (ZSET)**. Every job gets a score at submission time:

```
score = tier.priority * PRIORITY_BASE + arrivedAt (epoch ms)
```

| Tier   | Priority Value | Effect              |
|--------|----------------|---------------------|
| PAID   | 1              | Processed first     |
| UNPAID | 2              | Processed second    |
| PUBLIC | 3              | Processed last      |

`PRIORITY_BASE = 10^13` — large enough that tier dominates, timestamp breaks ties within the same tier.

Workers always pop the **lowest score** (`ZPOPMIN`) — highest priority job first.

**Retries preserve the original `arrivedAt`** — no unfair reordering. A retried job sits exactly where it would have if it had never failed.

---

### 3️⃣ Worker System

- Fixed thread pool (`ExecutorService`), size configurable via `application.yml`
- Workers loop continuously, popping jobs via `ZPOPMIN`
- On empty queue — short sleep, then retry
- Interrupt-aware shutdown via `@PreDestroy`

---

### 4️⃣ Heartbeat System

Every active job has a heartbeat running on a `ScheduledExecutorService`:

- Fires every **500ms**
- Updates score in `processing:by-heartbeat` ZSET
- New score = `now + tiered heartbeat timeout (epoch ms)`
- Update is atomic via **Lua script** — skips update if job is no longer `PROCESSING` (prevents race with scheduler)

---

### 5️⃣ Processing Store (Two Redis ZSETs)

| ZSET Key                  | Score                              | Purpose             |
|---------------------------|------------------------------------|---------------------|
| `processing:by-heartbeat` | `lastHeartbeat + heartbeatTimeout` | Crash detection     |
| `processing:by-maxtime`   | `startedAt + maxProcessingTime`    | Stuck job detection |

Scheduler queries: `ZRANGEBYSCORE key 0 {now}` — instantly returns all expired jobs. No iteration over all jobs needed.

---

### 6️⃣ Recovery Schedulers

Two independent schedulers, each with its own dynamic delay via `SystemHealthService`:

| Scheduler                   | Watches                   | Trigger           | Floor | Ceiling |
|-----------------------------|---------------------------|-------------------|-------|---------|
| `CrashedJobRecoveryService` | `processing:by-heartbeat` | Heartbeat expired | 3s    | 60s     |
| `StuckJobRecoveryService`   | `processing:by-maxtime`   | Max time exceeded | 5s    | 120s    |

Both call the same **atomic Lua recovery script** which:
1. Checks job status
2. Compares `retryCount` against tier-specific `maxRetries` (fetched from Redis config)
3. Within limit → increments retry, requeues with original score, removes from both ZSETs, sets status `PENDING`
4. Limit exhausted → sets status `NEED_DISCARD`, returns signal to Java

Java then calls `JobDiscardService` → writes to MySQL `failed_jobs` → cleanup.

> 📐 **Recovery Flow Diagram**
>
> ![Recovery Flow](docs/Recovery_Flow.png)

---

### 7️⃣ Tier-Based Retry Configuration

Each tier has independent thresholds — stored in Redis at startup, read by Lua scripts at runtime:

| Tier   | maxRetries | heartbeatTimeout | maxProcessingTime |
|--------|------------|------------------|-------------------|
| PUBLIC | 1          | 3s               | 10s               |
| UNPAID | 2          | 6s               | 20s               |
| PAID   | 5          | 15s              | 50s               |

---

### 8️⃣ Dead Letter Queue (DLQ)

Permanently failed jobs are written to MySQL `failed_jobs` table with:

| Field           | Description                                |
|-----------------|--------------------------------------------|
| `jobId`         | Unique job identifier                      |
| `jobTier`       | Tier at time of failure                    |
| `totalRetries`  | How many attempts were made                |
| `firstTriedAt`  | When a worker first picked it up           |
| `discardedAt`   | When it was permanently discarded          |
| `failureCause`  | `MAX_TIME_EXCEEDED` or `HEARTBEAT_STOPPED` |

---

### 9️⃣ Capacity Control (Redis Atomic Counter)

New job acceptance is governed by a Redis counter (not a JVM-local `Semaphore`) — making it safe across multiple instances:

- Lua script: check counter < capacity → increment → enqueue (atomic)
- On dequeue: if `retryCount == 0` (new job) → decrement counter
- Retried jobs **bypass the counter** — system has committed to the user via `jobId`

---

### 🔟 System Health Gate + Exponential Backoff

`SystemHealthService` tracks Redis health and drives two independent concerns:

| Concern               | Behaviour                                                                   |
|-----------------------|-----------------------------------------------------------------------------|
| **Acceptance gate**   | Rejects new job submissions (HTTP 503) when `failCount >= threshold`        |
| **Scheduler backoff** | Doubles delay on failure (up to ceiling), halves on success (down to floor) |

- Gate and backoff are **separate counters** — one cannot serve both without design compromises
- Retried jobs **bypass the health gate** — contract to user must be honoured
- Scheduler self-heals on Redis recovery — no external watchdog needed

---

## 🗄️ Persistence

| Store            | Technology                               | What lives here                                             |
|------------------|------------------------------------------|-------------------------------------------------------------|
| Job Metadata     | Redis Hash (`job:metadata:{jobId}`)      | status, retryCount, firstTriedAt, arrivedAt, jobTier        |
| Main Queue       | Redis ZSET (`main:queue`)                | jobId → priority score                                      |
| Processing Store | Redis ZSET × 2                           | Active jobs tracked by heartbeat expiry and max time expiry |
| System Config    | Redis Hash (`system:config:tier:{tier}`) | Per-tier thresholds, priority base — seeded at startup      |
| Completed Jobs   | MySQL (`job_results`)                    | jobId, result, timestamps, tier, retryCount                 |
| Failed Jobs      | MySQL (`failed_jobs`)                    | jobId, failureCause, timestamps, tier, retryCount           |

---

## 🚀 API Endpoints

### Submit Job

```
POST /paid/submit
POST /unpaid/submit
POST /public/submit
```

**Success (202):**
```json
{
  "apiResponseData": {
    "jobId": "019de340-01fe-7a58-be69-567cd7d6b7d4",
    "currentJobStatus": "PENDING"
  },
  "timestamp": "2026-05-01T11:31:57.961931Z"
}
```

**System unhealthy (503):**
```json
{
  "errorCode": "SYSTEM_UNHEALTHY",
  "message": "System is currently unavailable due to high load. Please try again later.",
  "timestamp": "2026-05-01T11:31:57.961931Z"
}
```

**Queue full (SERVICE UNAVAILABLE 503):**
```json
{
  "errorCode": "QUEUE_FULL",
  "message": "Request cannot be accepted at the moment due to high load. Please try again later.",
  "timestamp": "2026-05-01T11:31:57.961931Z"
}
```

---

### Poll Result

```
GET /result/poll?jobId={jobId}
```

---

## 🧱 Tech Stack

| Component         | Technology                                |
|-------------------|-------------------------------------------|
| Language          | Java 21                                   |
| Framework         | Spring Boot                               |
| Distributed Cache | Redis (via Lettuce + Spring Data Redis)   |
| Atomic Operations | Lua scripts executed via Redis            |
| Persistence       | MySQL (Spring Data JPA + Hibernate)       |
| Build Tool        | Maven                                     |
| Containerisation  | Docker (Redis + MySQL via Docker Compose) |

---

## 🛠️ Prerequisites

Before starting, ensure you have the following installed:

- **Java 21** (Hotspot/Adoptium recommended)
- **Docker & Docker Compose**
- **Maven 3.9+**
- An API client like **Postman** or **cURL**

---

## 🏁 Getting Started

### Step 1: Environment Configuration

The `docker-compose.yml` uses environment variables. Create a `.env` file in the root directory:

```bash
# .env
MYSQL_DATABASE=traffic_control_service_db
MYSQL_ROOT_PASSWORD=your_secure_password
```

Then update `src/main/resources/application.yml` to match these credentials so Spring Boot can connect to the running containers.

---

### Step 2: Spin Up Infrastructure

Launch the database and cache layers. The `-d` flag runs them in the background:

```bash
docker-compose up -d
```

> **Note:** On first run, wait ~10–15 seconds for MySQL to finish initializing its internal file system. Check container health with `docker ps`.

---

### Step 3: Build and Run

```bash
mvn clean install
mvn spring-boot:run
```

Look for the log entry:

```
Started TrafficControlServiceApplication in X seconds...
Starting 3 worker threads...
```

---

### Step 4: Verify & Test

Once the service is live on port **8080**, fire a test request against the Paid tier to see the priority logic in action.

**1. Submit a job:**

```bash
curl -X POST http://localhost:8080/paid/submit
```

Expected response: `202 Accepted` with a `jobId`.

**2. Poll for result** (replace `{jobId}` with the value from the previous step):

```bash
curl -X GET "http://localhost:8080/result/poll?jobId={jobId}"
```

---

### Step 5: Shutdown

Stop and remove containers while keeping your data volumes intact:

```bash
docker-compose down
```

---

## 📦 Package Structure

```
controllers/     → tier-specific submit endpoints + shared result controller
services/        → all business logic
models/          → internal domain objects
dtos/            → API request/response boundary objects
entity/          → JPA entities (JobResult, FailedJob)
advices/         → global exception handling
enums/           → JobStatus, FailureCause, JobTier
constants/       → RedisKeys, LuaScripts, system constants
config/          → SystemConfigs, RedisConfig, scheduler wiring
```

---

## 🧠 Key Design Decisions

| Decision                       | Reasoning                                                                                                    |
|--------------------------------|--------------------------------------------------------------------------------------------------------------|
| Redis ZSET for queue           | Native priority ordering, distributed, atomic pop                                                            |
| Two processing ZSETs           | Clean separation of crash vs stuck detection — single ZSET cannot serve both                                 |
| Lua scripts for atomicity      | Redis executes Lua atomically — eliminates check-then-act race conditions across all critical paths          |
| `arrivedAt` preserved on retry | Fairness — retried jobs don't skip the line                                                                  |
| Tier config in Redis           | Lua scripts can read thresholds directly — no Java roundtrip needed inside atomic operations                 |
| Capacity via Redis counter     | JVM-local `Semaphore` breaks in multi-instance deployments                                                   |
| MySQL for results + DLQ        | Durable, queryable, survives Redis restarts                                                                  |
| TTL on metadata                | Completed jobs expire after 24h, failed after 1h — prevents unbounded Redis growth                           |
| Two independent schedulers     | Separate recovery frequencies per failure type — crash detection is more time-sensitive than stuck detection |

---

## 🖥️ Web Interface

A lightweight frontend is included for demonstration purposes, accessible at `http://localhost:8080` after starting the application.

| Page        | URL       | Purpose                                           |
|-------------|-----------|---------------------------------------------------|
| Overview    | `/`       | System architecture, how it works, tier reference |
| Submit Job  | `/submit` | Submit a job by tier, receive a `jobId`           |
| Poll Result | `/poll`   | Check job status and retrieve result by `jobId`   |
| Demo        | `/demo`   | Animated walkthrough of the system internals      |
| About       | `/about`  | Creator info and links                            |

---

## 🛣️ Roadmap

- [x] Async processing system
- [x] Priority queue (tier-based scoring)
- [x] Heartbeat system
- [x] Retry + recovery schedulers (crash + stuck)
- [x] Dead Letter Queue
- [x] System health gate + exponential backoff
- [x] Redis integration (queue, metadata, processing store)
- [x] Atomic Lua scripts
- [x] MySQL persistence (results + DLQ)
- [x] Tier-based configuration (per-tier retry, timeout, heartbeat)

---

## 🔮 Future enhancements:

- [ ] Rate limiting
- [ ] Observability (metrics + tracing)
- [ ] Distributed workers (multi-instance deployment)

---

## 👨‍💻 Author

**Vishal Sharma**

- 🔗 [LinkedIn](https://www.linkedin.com/in/vishal-sharma87/)
- 🐙 [GitHub](https://github.com/Vishal-Sharma87)

---

## 📄 License

For learning and demonstration purposes. Not intended for production use without significant modifications and testing.