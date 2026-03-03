# 🚦 Traffic Control Service

A distributed API traffic control system built with **Spring Boot** and **Redis**, designed to provide rate limiting, IP-based DDoS protection, and soft throttling using queue-based backpressure.

This project is designed as an infrastructure-level backend component suitable for high-throughput microservice environments.

---

## 🎯 Objective

To build a scalable and extensible traffic control layer that:

* Controls request flow using token-based rate limiting
* Prevents IP-based abuse and DDoS patterns
* Supports distributed deployment
* Implements soft throttling via queueing
* Maintains high-performance request filtering

---

## 🏗️ Architecture Overview

```
Client Request
      ↓
Traffic Filter Layer
      ↓
 ┌───────────────┐
 │ Rate Limiter  │ ← Redis-backed token bucket
 └───────────────┘
      ↓
 ┌───────────────┐
 │ DDoS Guard    │ ← IP request counters
 └───────────────┘
      ↓
 ┌───────────────┐
 │ Soft Queue    │ ← Redis List/Stream
 └───────────────┘
      ↓
Request Processing
```

---

## ⚙️ Core Features

### 1️⃣ Token Bucket Rate Limiting

* Redis-backed distributed counter
* Atomic operations (Lua support planned)
* Configurable refill strategy
* Per-IP / Per-API key limiting

---

### 2️⃣ IP-Based DDoS Protection

* Sliding window request counting
* Auto-block via TTL
* Temporary IP blacklist support
* Fast rejection path

---

### 3️⃣ Soft Throttling via Queue

* Overflow requests pushed to Redis queue
* Background worker drains queue
* Prevents hard drops during traffic spikes

---

### 4️⃣ High Performance Processing

* Minimal blocking operations
* Reduced Redis round-trips
* Optimized key structure
* Fast-path approval when tokens available

---

## 🧱 Technology Stack

* **Java 21**
* **Spring Boot**
* **Redis**
* **Maven**
* **Docker (for Redis)**

---

## 🔑 Redis Key Design (Planned)

| Purpose        | Key Pattern              |
| -------------- | ------------------------ |
| Token Bucket   | `rate:ip:{client-ip}`    |
| Sliding Window | `window:ip:{client-ip}`  |
| Blocklist      | `blocked:ip:{client-ip}` |
| Queue          | `queue:traffic`          |

---

## 🚀 Getting Started

### 1️⃣ Start Redis

Using Docker:

```
docker run -p 6379:6379 redis
```

---

### 2️⃣ Configure Environment Variables

```
export REDIS_HOST=localhost
export REDIS_PORT=6379
```

---

### 3️⃣ Run Application

```
mvn spring-boot:run
```

---

## 📌 Configuration

`application.yml` uses environment placeholders:

```yaml
spring:
  redis:
    host: ${REDIS_HOST}
    port: ${REDIS_PORT}
```

Local environment-specific YAML files are intentionally excluded from version control.

---

## 🧠 Design Goals

* Horizontally scalable
* Safe for distributed deployments
* Minimal shared memory reliance
* Clean separation of traffic control from business logic
* Interview-ready system design demonstration

---

## 🛣️ Roadmap

* [ ] Lua-based atomic token handling
* [ ] Sliding window algorithm implementation
* [ ] Distributed instance simulation
* [ ] Circuit breaker integration
* [ ] Metrics & observability (Micrometer)
* [ ] Prometheus integration
* [ ] Dashboard for traffic visualization

---

## 📚 Learning Focus

This project explores:

* Distributed rate limiting patterns
* Redis atomic operations
* High-concurrency request management
* Backend infrastructure design
* Defensive system architecture

---

## 👨‍💻 Author

Vishal Sharma
---

## 📄 License

For learning and demonstration purposes.
