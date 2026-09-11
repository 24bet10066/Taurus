#!/bin/bash
# Starts the 7 ServiceOS Spring Boot microservices, each in its own macOS Terminal window.
# Assumes infra (docker compose) and shared-lib are already up.
# Usage: ./start-serviceos.sh

set -euo pipefail

PROJECT_DIR="$HOME/Documents/Taurus"
JWT="sk-electronics-banda-super-secret-jwt-key-2024-secure"
DB_USER="postgres"
DB_PASS="postgres"
DB_HOST="localhost:5432"

if ! command -v osascript >/dev/null 2>&1; then
  echo "ERROR: osascript not found. This script requires macOS Terminal."
  exit 1
fi

# Sanity-check service dirs before spawning any windows.
for svc in auth-service customer-service technician-service parts-service job-service payment-service analytics-service; do
  if [ ! -d "${PROJECT_DIR}/${svc}" ]; then
    echo "ERROR: missing directory ${PROJECT_DIR}/${svc}"
    exit 1
  fi
done

launch() {
  local title="$1"
  local cmd="$2"
  # \033]0;TITLE\007 sets the Terminal tab title so all 7 windows are identifiable at a glance.
  osascript <<APPLE
tell application "Terminal"
    activate
    do script "printf '\\033]0;${title}\\007'; ${cmd}"
end tell
APPLE
  sleep 0.6   # small stagger so Terminal spawns each window cleanly
}

echo "Launching 7 ServiceOS services in separate Terminal windows..."
echo ""

# 1. auth-service (port 8081)
launch "auth-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_auth?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} && cd ${PROJECT_DIR}/auth-service && mvn spring-boot:run"

# 2. customer-service (port 8084)
launch "customer-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_customers?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} && cd ${PROJECT_DIR}/customer-service && mvn spring-boot:run"

# 3. technician-service (port 8085)
launch "technician-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_technicians?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} && cd ${PROJECT_DIR}/technician-service && mvn spring-boot:run"

# 4. parts-service (port 8083)
launch "parts-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_parts?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} && cd ${PROJECT_DIR}/parts-service && mvn spring-boot:run"

# 5. job-service (port 8082) — depends on customer / technician / parts
launch "job-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_jobs?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} TECHNICIAN_SERVICE_URL=http://localhost:8085 PARTS_SERVICE_URL=http://localhost:8083 CUSTOMER_SERVICE_URL=http://localhost:8084 && cd ${PROJECT_DIR}/job-service && mvn spring-boot:run"

# 6. payment-service (port 8087) — depends on job-service
launch "payment-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_payments?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} RAZORPAY_KEY_ID=test_key RAZORPAY_KEY_SECRET=test_secret RAZORPAY_WEBHOOK_SECRET=test_webhook JOB_SERVICE_URL=http://localhost:8082 && cd ${PROJECT_DIR}/payment-service && mvn spring-boot:run"

# 7. analytics-service (port 8088)
launch "analytics-service" \
  "export JWT_SECRET=${JWT} SPRING_DATASOURCE_URL=jdbc:postgresql://${DB_HOST}/serviceos_analytics?sslmode=disable SPRING_DATASOURCE_USERNAME=${DB_USER} SPRING_DATASOURCE_PASSWORD=${DB_PASS} && cd ${PROJECT_DIR}/analytics-service && mvn spring-boot:run"

echo "All 7 launch commands sent to Terminal."
echo "Each window will print: 'Started XxxApplication in X seconds' when ready."
echo "DO NOT close those windows — keep them open while developing."
echo ""
echo "Ports: auth=8081  jobs=8082  parts=8083  customers=8084"
echo "       technicians=8085  payments=8087  analytics=8088"
echo ""
echo "Once all are up, run:  ./health-check.sh"
