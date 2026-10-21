#!/usr/bin/env bash
set -euo pipefail
set -x

ROOT="$(cd "$(dirname "$0")/../.." && pwd)"
cd "$ROOT"
RUN_DIR="$ROOT/benchmarks/phase3"
TRANSCRIPT="$RUN_DIR/run_transcript.txt"
exec > >(tee -a "$TRANSCRIPT") 2>&1

set -a
. "$ROOT/.env"
set +a

redis() { docker exec tcs-redis redis-cli "$@"; }
mysql() { docker exec -e MYSQL_PWD="$MYSQL_ROOT_PASSWORD" tcs-mysql mysql -uroot --batch --skip-column-names 'tcs-db' -e "$1"; }
now_ms() { date +%s%N | cut -c1-13; }
state() {
  printf 'main_queue='; redis ZCARD 'main:queue:'
  printf 'processing_heartbeat='; redis ZCARD 'processing:byHeartbeat:'
  printf 'processing_started_at='; redis ZCARD 'processing:byStartedAt:'
  printf 'capacity_counter='; redis GET 'system:capacity:'
  printf 'job_results='; mysql 'SELECT COUNT(*) FROM job_results;'
  printf 'failed_jobs='; mysql 'SELECT COUNT(*) FROM failed_jobs;'
}

echo "$(date -Is) docker verification"
docker ps --format 'table {{.Names}}\t{{.Image}}\t{{.Ports}}\t{{.Status}}'
test "$(docker inspect -f '{{.Name}}' tcs-redis)" = '/tcs-redis'
test "$(docker inspect -f '{{.Name}}' tcs-mysql)" = '/tcs-mysql'
test "${REDIS_HOST}" = 'redis'

echo "$(date -Is) destructive cleanup of verified local containers"
mysql 'TRUNCATE TABLE job_results; TRUNCATE TABLE failed_jobs;'
redis FLUSHALL
state > "$RUN_DIR/pre_state.txt"

BASE_LOG_LINES=$(wc -l < mid_processing_kill_logs.log)
echo "$BASE_LOG_LINES" > "$RUN_DIR/app_log_start_line.txt"
if pgrep -f 'com\.vishal\.traffic_control_service\.TrafficControlServiceApplication' >/dev/null; then
  echo 'APPLICATION_ALREADY_RUNNING_BEFORE_MEASUREMENT' >&2
  exit 1
fi

echo "$(date -Is) start command: REDIS_HOST=localhost ./mvnw spring-boot:run 2>&1 | tee -a phase3_app.log"
REDIS_HOST=localhost ./mvnw spring-boot:run 2>&1 | tee -a mid_processing_kill_logs.log &
MAVEN_PIPE_PID=$!
for i in $(seq 1 120); do
  if tail -n +$((BASE_LOG_LINES+1)) mid_processing_kill_logs.log | grep -q 'Started TrafficControlServiceApplication' && curl -fsS http://localhost:8080/public/health > "$RUN_DIR/health_response.txt"; then break; fi
  sleep 1
done
test "$(cat "$RUN_DIR/health_response.txt")" = 'OK :)'

: > "$RUN_DIR/submit_log.csv"
: > "$RUN_DIR/accepted_ids.txt"
echo 'seq,epoch_ms,http_status,job_id' > "$RUN_DIR/submit_log.csv"
accepted=0
for i in $(seq 1 100); do
  now=$(now_ms)
  response=$(curl -sS -w '\n%{http_code}' -X POST http://localhost:8080/public/submit)
  status=$(printf '%s\n' "$response" | tail -n 1)
  body=$(printf '%s\n' "$response" | sed '$d')
  job_id=$(printf '%s' "$body" | sed -n 's/.*"jobId"[[:space:]]*:[[:space:]]*"\([^"]*\)".*/\1/p')
  echo "$i,$now,$status,$job_id" >> "$RUN_DIR/submit_log.csv"
  if [ "$status" = 202 ]; then printf '%s\n' "$job_id" >> "$RUN_DIR/accepted_ids.txt"; accepted=$((accepted+1)); fi
done
echo "accepted_count=$accepted" | tee "$RUN_DIR/accepted_count.txt"

sleep 20
state > "$RUN_DIR/pre_kill_state.txt"
wc -l mid_processing_kill_logs.log | tee "$RUN_DIR/pre_kill_log_lines.txt"

APP_PID=$(pgrep -f 'com\.vishal\.traffic_control_service\.TrafficControlServiceApplication' | head -n 1)
test -n "$APP_PID"
pgrep -af 'com\.vishal\.traffic_control_service\.TrafficControlServiceApplication'
KILL_MS=$(now_ms)
echo "pid=$APP_PID epoch_ms=$KILL_MS" | tee "$RUN_DIR/kill_event.txt"
kill -9 "$APP_PID"
sleep 2
if kill -0 "$APP_PID" 2>/dev/null; then echo 'JVM_STILL_ALIVE'; exit 1; else echo 'JVM_GONE'; fi | tee "$RUN_DIR/jvm_gone.txt"

RESTART_MS=$(now_ms)
echo "restart_epoch_ms=$RESTART_MS" | tee "$RUN_DIR/restart_event.txt"
echo "$(date -Is) restart command: REDIS_HOST=localhost ./mvnw spring-boot:run 2>&1 | tee -a phase3_app.log"
REDIS_HOST=localhost ./mvnw spring-boot:run 2>&1 | tee -a mid_processing_kill_logs.log &
RESTART_PIPE_PID=$!
RESTART_BASE_LINES=$(wc -l < mid_processing_kill_logs.log)
for i in $(seq 1 120); do
  if tail -n +$((RESTART_BASE_LINES+1)) mid_processing_kill_logs.log | grep -q 'Started TrafficControlServiceApplication' && curl -fsS http://localhost:8080/public/health > "$RUN_DIR/health_response_restart.txt"; then break; fi
  sleep 1
done
test "$(cat "$RUN_DIR/health_response_restart.txt")" = 'OK :)'
wc -l mid_processing_kill_logs.log | tee "$RUN_DIR/post_restart_log_lines.txt"

drain_start=$(date +%s)
zero_streak=0
: > "$RUN_DIR/poll_log.txt"
while [ $(( $(date +%s) - drain_start )) -lt 600 ]; do
  poll_ts=$(now_ms)
  mq=$(redis ZCARD 'main:queue:')
  hb=$(redis ZCARD 'processing:byHeartbeat:')
  st=$(redis ZCARD 'processing:byStartedAt:')
  echo "$poll_ts main_queue=$mq processing_heartbeat=$hb processing_started_at=$st" | tee -a "$RUN_DIR/poll_log.txt"
  if [ "$mq" = 0 ] && [ "$hb" = 0 ] && [ "$st" = 0 ]; then zero_streak=$((zero_streak+1)); else zero_streak=0; fi
  if [ "$zero_streak" -ge 3 ]; then break; fi
  sleep 5
done
drain_end=$(now_ms)
echo "restart_epoch_ms=$RESTART_MS drain_epoch_ms=$drain_end time_to_drain_ms=$((drain_end-RESTART_MS)) zero_streak=$zero_streak" | tee "$RUN_DIR/drain_result.txt"
test "$zero_streak" -ge 3
