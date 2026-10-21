# Run configuration

- Branch: bench/phase3
- Commit: 341a64f2dab7cb2b3db508bc6028d05690e58ed3
- Startup: `REDIS_HOST=localhost ./mvnw spring-boot:run`
- Main queue capacity: 500
- Workers: 3; heartbeat workers: 3
- Heartbeat: initial 0 ms, interval 500 ms
- Scheduler stuck floor/ceiling: 5000/120000 ms
- Scheduler crash floor/ceiling: 5000/60000 ms
- Job sleep: existing 2000 ms sleep, no exception
- Workload: 100 sequential POST /public/submit requests
- Infrastructure: local Docker containers tcs-redis and tcs-mysql
