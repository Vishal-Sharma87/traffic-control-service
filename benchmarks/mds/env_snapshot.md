# Phase 3 discovery snapshot

- Snapshot timestamp: 2026-10-19T18:39:00+05:30
- Git commit: `341a64f2dab7cb2b3db508bc6028d05690e58ed3`
- Branch: `bench/phase3`
- Worktree: pre-existing dirty changes were present before this protocol.
- Startup: `REDIS_HOST=localhost ./mvnw spring-boot:run` (the app runs on the host; Docker publishes Redis on localhost:6379 and MySQL on localhost:3306). Output is appended with `2>&1 | tee -a phase3_app.log`.
- Readiness endpoint: `GET http://localhost:8080/public/health`, expected body `OK :)`.
- Submit endpoint: `POST http://localhost:8080/public/submit`.
- Submit response: HTTP 202; JSON shape contains `apiResponseData.jobId` and `apiResponseData.currentJobStatus` (`PENDING`), plus `timestamp`.
- Redis: Docker container `tcs-redis`, image `redis:7.2-alpine`, published `localhost:6379`; app runtime override is `REDIS_HOST=localhost`, port 6379.
- MySQL: Docker container `tcs-mysql`, image `mysql:8.0`, published `localhost:3306`; database/user from `../../.env` are `tcs-db`, `root`; password is present in `../../.env` but intentionally redacted from artifacts.
- Docker verification: `tcs-redis` and `tcs-mysql` were both healthy local containers.
- Main queue capacity: 500.
- Workers: 3 job workers; 3 heartbeat workers.
- Scheduler floor/ceiling: stuck 5000/120000 ms; crash 5000/60000 ms.
- Heartbeat: initial delay 0 ms; interval 500 ms.
- Tier config: PAID processing/heartbeat/retries 50000/15000/5 ms/count; UNPAID 20000/6000/2; PUBLIC 10000/3000/1.
- Hikari pool: no explicit Hikari pool-size setting was found; active log reports minimum/maximum pool size `undefined/unknown` (driver default applies).
- Simulated job: existing worktree change in `JobService.java` sleeps 2 seconds and throws no exception; no source change made by this protocol.
