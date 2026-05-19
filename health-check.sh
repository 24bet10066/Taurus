#!/bin/bash
# Health check for the 7 ServiceOS Spring Boot services.
# Hits each /actuator/health and prints UP/DOWN.
# Usage: ./health-check.sh

echo "ServiceOS health check — $(date)"
echo ""
printf "%-5s  %-20s  %s\n" "Port" "Service" "Status"
printf "%-5s  %-20s  %s\n" "----" "--------------------" "------"

# port:service-name pairs (in the same order as start-serviceos.sh)
entries=(
  "8081:auth-service"
  "8082:job-service"
  "8083:parts-service"
  "8084:customer-service"
  "8085:technician-service"
  "8087:payment-service"
  "8088:analytics-service"
)

up=0
down=0
for entry in "${entries[@]}"; do
  port="${entry%%:*}"
  name="${entry##*:}"
  resp=$(curl -s --max-time 2 "http://localhost:${port}/actuator/health" 2>/dev/null || true)
  status=$(echo "$resp" | grep -o '"status":"[^"]*"' | head -1 | sed 's/.*"status":"\([^"]*\)".*/\1/')
  if [ -z "$status" ]; then
    status="DOWN (no response)"
    down=$((down+1))
  elif [ "$status" = "UP" ]; then
    up=$((up+1))
  else
    down=$((down+1))
  fi
  printf "%-5s  %-20s  %s\n" "$port" "$name" "$status"
done

echo ""
echo "Summary: ${up} UP / ${down} not-UP (of 7)"
[ "$down" -eq 0 ] && exit 0 || exit 1
