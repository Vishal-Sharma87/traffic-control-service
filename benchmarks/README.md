# Phase 3 benchmark

This benchmark tested conservation after a hard JVM crash and restart for a single Spring Boot instance using Redis via Lettuce and MySQL via JPA.

Conditions: local Docker containers `tcs-redis` and `tcs-mysql`; existing simulated 2000 ms job sleep with no exception; 100 sequential public submissions; 3 job workers; 3 heartbeat workers; main queue capacity 500; heartbeat initial delay 0 ms and interval 500 ms; public heartbeat timeout 3000 ms; crash scheduler floor/ceiling 5000/60000 ms; public retry limit 1.

The valid run accepted 100 jobs, killed JVM PID 43113 mid-run, restarted the app, and drained in 74663 ms. It produced 100 job results, 0 failed jobs, zero residual queue/processing ZSET entries, capacity counter 0, and three normalized duplicate executions explained by pre-kill in-flight jobs retried after restart.

Not verified: multiple simultaneous app instances, network or Docker node failure, persistence after Redis/MySQL container loss, non-public tiers, and long-term metadata TTL behavior.

Rerun from the project root with local Docker containers healthy:

```bash
benchmarks/scripts/mid_processing_kill.sh
benchmarks/scripts/collect_mid_processing_kill.sh
```

The runner requires no application JVM to be running before it starts. It reads `../.env` locally; secrets are not copied into benchmark artifacts.
