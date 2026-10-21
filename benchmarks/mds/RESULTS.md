# Phase 3 conservation test results

Valid run: 2026-10-19, branch `bench/phase3`, commit `341a64f2dab7cb2b3db508bc6028d05690e58ed3`.

| Criterion                                                | Result | Raw evidence                                                                                              |
|----------------------------------------------------------|--------|-----------------------------------------------------------------------------------------------------------|
| 1. `job_results + failed_jobs = N`; ID differences empty | PASS   | N accepted = 100; `job_results` = 100; `failed_jobs` = 0; missing IDs = 0 lines; unexpected IDs = 0 lines |
| 2. All three ZSETs zero and admission counter zero       | PASS   | `main:queue:` = 0; `processing:byHeartbeat:` = 0; `processing:byStartedAt:` = 0; `system:capacity:` = 0   |
| 3. Join count zero                                       | PASS   | `job_results JOIN failed_jobs` = 0                                                                        |
| 4. Every duplicate execution explained                   | PASS   | 3 normalized duplicate pickups; each was picked before kill and again after restart                       |

Raw run numbers:

- Pre-run: queue 0, heartbeat ZSET 0, started-at ZSET 0, capacity counter empty, job_results 0, failed_jobs 0.
- At kill: queue 64, heartbeat ZSET 3, started-at ZSET 3, capacity counter 64, job_results 33, failed_jobs 0.
- Kill: JVM PID 43113 at epoch ms 1792415616467.
- Restart: epoch ms 1792415618487.
- Drain: epoch ms 1792415693150; measured time-to-drain 74663 ms; three consecutive zero polls.
- Post-run: queue 0, both processing ZSETs 0, capacity counter 0.

Duplicate executions (normalized for the observed double-written log lines):

- `01a1544b-7353-7818-b77b-a55003372b17`: pre-kill pickup 1792415614605; post-restart pickup 1792415640842.
- `01a1544b-7371-740e-b730-4059a39ba13f`: pre-kill pickup 1792415614643; post-restart pickup 1792415640848.
- `01a1544b-7390-7be8-a2fc-8088441ce25f`: pre-kill pickup 1792415614651; post-restart pickup 1792415640860.

The raw duplicate file reports 4 log lines for each of those jobs because every app log record was written once by
Spring's configured file appender and once by the required `tee -a`; normalized pickup counts are 2. No failure-cause
rows were returned because `failed_jobs` was empty.
