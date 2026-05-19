#!/bin/bash

ROOT="$(cd "$(dirname "$0")" && pwd)"
PID_DIR="$ROOT/.pids"

# Reverse dependency order — gateway first, auth last
ORDERED=(
  api-gateway
  analytics-service
  notification-service
  payment-service
  job-service
  technician-service
  customer-service
  auth-service
)

stop_service() {
  local svc="$1"
  local pid_file="$PID_DIR/$svc.pid"

  if [ ! -f "$pid_file" ]; then
    echo "  $svc: no PID file, skipping."
    return
  fi

  local pid
  pid=$(cat "$pid_file")

  if ! kill -0 "$pid" 2>/dev/null; then
    echo "  $svc: already stopped (stale PID $pid)."
    rm -f "$pid_file"
    return
  fi

  echo "  Stopping $svc (PID $pid)..."

  # Kill the mvn process and its entire process group (catches the child JVM)
  local pgid
  pgid=$(ps -o pgid= -p "$pid" 2>/dev/null | tr -d ' ')
  if [ -n "$pgid" ] && [ "$pgid" != "0" ]; then
    kill -- -"$pgid" 2>/dev/null || true
  else
    kill "$pid" 2>/dev/null || true
  fi

  # Wait up to 10 s for the process to exit
  local waited=0
  while kill -0 "$pid" 2>/dev/null && [ $waited -lt 10 ]; do
    sleep 1
    waited=$((waited + 1))
  done

  if kill -0 "$pid" 2>/dev/null; then
    echo "  $svc did not exit cleanly — sending SIGKILL..."
    kill -9 "$pid" 2>/dev/null || true
  fi

  rm -f "$pid_file"
  echo "  ✓ $svc stopped."
}

echo ""
echo "▶ Stopping services..."
for svc in "${ORDERED[@]}"; do
  stop_service "$svc"
done

echo ""
echo "All services stopped."
echo "Infrastructure (Docker) is still running."
echo "To stop Docker:  docker-compose down"
echo "To wipe volumes: docker-compose down -v"
