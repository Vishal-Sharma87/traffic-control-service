# submits 100 jobs (HTTP 202s as N)
for i in $(seq 1 100); do
    curl -s -o /dev/null -w "%{http_code}\n" -X POST localhost:8080/public/submit;
    done | sort | uniq -c > submit_codes.txt
sleep 20
kill -9 $(pgrep -f traffic-control-service)