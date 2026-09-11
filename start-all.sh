#!/bin/bash
set -e

mkdir -p ~/Documents/Taurus/logs

JWT="sk-electronics-banda-super-secret-jwt-key-2024-secure"
PG="postgres"
HOST="localhost:5432"

echo "Starting all ServiceOS services..."

start_service() {
  local name=$1
  local db=$2
  local extra=$3
  local port=$4

  echo -n "Starting $name on port $port... "

  eval "export JWT_SECRET=$JWT \
    SPRING_DATASOURCE_URL=jdbc:postgresql://$HOST/$db?sslmode=disable \
    SPRING_DATASOURCE_USERNAME=$PG \
    SPRING_DATASOURCE_PASSWORD=$PG \
    $extra"

  nohup mvn spring-boot:run -f ~/Documents/Taurus/$name/pom.xml \
    > ~/Documents/Taurus/logs/$name.log 2>&1 &

  echo "PID $! → logs/$name.log"
}

start_service "auth-service"        "serviceos_auth"        "" "8081"
sleep 3
start_service "customer-service"    "serviceos_customers"   "" "8084"
start_service "technician-service"  "serviceos_technicians" "" "8085"
start_service "parts-service"       "serviceos_parts"       "" "8083"
sleep 3
start_service "job-service"         "serviceos_jobs" \
  "TECHNICIAN_SERVICE_URL=http://localhost:8085 PARTS_SERVICE_URL=http://localhost:8083 CUSTOMER_SERVICE_URL=http://localhost:8084" \
  "8082"
start_service "payment-service"     "serviceos_payments" \
  "RAZORPAY_KEY_ID=test_key RAZORPAY_KEY_SECRET=test_secret RAZORPAY_WEBHOOK_SECRET=test_webhook JOB_SERVICE_URL=http://localhost:8082" \
  "8087"
start_service "analytics-service"   "serviceos_analytics"   "" "8088"

echo ""
echo "All services started. Waiting 60 seconds for boot..."
sleep 60
echo "Running health check..."
~/Documents/Taurus/health-check.sh
