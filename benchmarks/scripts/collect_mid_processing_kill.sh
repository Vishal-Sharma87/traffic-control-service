#!/usr/bin/env bash
set -euo pipefail
set -x
ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
RUN_DIR="$ROOT/benchmarks/phase3"
TRANSCRIPT="$RUN_DIR/collect_transcript.txt"
exec > >(tee -a "$TRANSCRIPT") 2>&1
set -a
. "$ROOT/.env"
set +a
redis() { docker exec tcs-redis redis-cli "$@"; }
mysql() { docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" tcs-mysql mysql -uroot --batch --skip-column-names 'tcs-db' -e "$1"; }

{
  echo "main:queue: $(redis ZCARD 'main:queue:')"
  echo "processing:byHeartbeat: $(redis ZCARD 'processing:byHeartbeat:')"
  echo "processing:byStartedAt: $(redis ZCARD 'processing:byStartedAt:')"
  echo "system:capacity: $(redis GET 'system:capacity:')"
  echo 'sample metadata (missing keys are noted):'
  head -n 5 "$RUN_DIR/accepted_ids.txt" | while read -r id; do
    echo "job:metadata:$id"
    if [ "$(redis EXISTS "job:metadata:$id")" = 1 ]; then redis HGETALL "job:metadata:$id"; else echo 'MISSING (TTL may have expired)'; fi
  done
} > "$RUN_DIR/post_state.txt"

{
  echo 'job_results_count:'; mysql 'SELECT COUNT(*) FROM job_results;'
  echo 'failed_jobs_by_cause:'; mysql 'SELECT failure_cause, COUNT(*) FROM failed_jobs GROUP BY failure_cause;'
  echo 'join_count:'; mysql 'SELECT COUNT(*) FROM job_results r JOIN failed_jobs f ON r.job_id=f.job_id;'
} > "$RUN_DIR/sql_results.txt"
UUID_SQL='LOWER(CONCAT(SUBSTR(HEX(job_id),1,8),"-",SUBSTR(HEX(job_id),9,4),"-",SUBSTR(HEX(job_id),13,4),"-",SUBSTR(HEX(job_id),17,4),"-",SUBSTR(HEX(job_id),21,12)))'
mysql "SELECT $UUID_SQL FROM job_results ORDER BY job_id;" > "$RUN_DIR/job_results_ids.txt"
mysql "SELECT $UUID_SQL FROM failed_jobs ORDER BY job_id;" > "$RUN_DIR/failed_jobs_ids.txt"
sort -u "$RUN_DIR/job_results_ids.txt" "$RUN_DIR/failed_jobs_ids.txt" > "$RUN_DIR/table_union_ids.txt"
sort -u "$RUN_DIR/accepted_ids.txt" > "$RUN_DIR/accepted_ids_sorted.txt"
comm -23 "$RUN_DIR/accepted_ids_sorted.txt" "$RUN_DIR/table_union_ids.txt" > "$RUN_DIR/missing_ids.txt"
comm -13 "$RUN_DIR/accepted_ids_sorted.txt" "$RUN_DIR/table_union_ids.txt" > "$RUN_DIR/unexpected_ids.txt"

BASE=$(cat "$RUN_DIR/app_log_start_line.txt")
awk -v b="$BASE" 'NR>b && /Job found, jobId:/ {print}' phase3_app.log > "$RUN_DIR/pickup_lines.txt"
{
  echo 'duplicate execution counts:'
  while read -r id; do
    [ -n "$id" ] || continue
    count=$(grep -F "Job found, jobId: $id," "$RUN_DIR/pickup_lines.txt" | wc -l)
    unique_count=$(grep -F "Job found, jobId: $id," "$RUN_DIR/pickup_lines.txt" | awk '!seen[$0]++' | wc -l)
    echo "$id raw_log_line_count=$count normalized_pickup_count=$unique_count"
    if [ "$unique_count" -gt 1 ]; then grep -F "Job found, jobId: $id," "$RUN_DIR/pickup_lines.txt" | awk '!seen[$0]++'; fi
  done < "$RUN_DIR/accepted_ids.txt"
} > "$RUN_DIR/duplicates.txt"

PRE=$(cat "$RUN_DIR/pre_kill_log_lines.txt" | awk '{print $1}')
POST=$(cat "$RUN_DIR/post_restart_log_lines.txt" | awk '{print $1}')
{
  for label in 'status mismatch' 'Crash Job detected and will be retried' 'no more retries' 'ERROR'; do
    echo "[$label]"
    printf 'before_kill='; sed -n "$((BASE+1)),$PRE p" phase3_app.log | { grep -F "$label" || true; } | wc -l
    printf 'after_restart='; sed -n "$((POST+1)),\$p" phase3_app.log | { grep -F "$label" || true; } | wc -l
  done
} > "$RUN_DIR/guard_counts.txt"

{
  echo '# Run configuration'
  echo
  echo "- Branch: $(git branch --show-current)"
  echo "- Commit: $(git rev-parse HEAD)"
  echo '- Startup: `REDIS_HOST=localhost ./mvnw spring-boot:run`'
  echo "- Main queue capacity: $MAIN_QUEUE_CAPACITY"
  echo "- Workers: $JOB_WORKERS_COUNT; heartbeat workers: $HEARTBEAT_WORKER_COUNT"
  echo "- Heartbeat: initial $HEARTBEAT_INITIAL_DELAY ms, interval $HEARTBEAT_INTERVAL ms"
  echo "- Scheduler stuck floor/ceiling: $STUCK_SCHEDULER_FLOOR_MS/$STUCK_SCHEDULER_CEILING_MS ms"
  echo "- Scheduler crash floor/ceiling: $CRASHED_SCHEDULER_FLOOR_MS/$CRASHED_SCHEDULER_CEILING_MS ms"
  echo '- Job sleep: existing 2000 ms sleep, no exception'
  echo '- Workload: 100 sequential POST /public/submit requests'
  echo '- Infrastructure: local Docker containers tcs-redis and tcs-mysql'
} > "$RUN_DIR/run_config.md"
