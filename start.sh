#!/bin/bash
set -e

echo "Step 1: Starting infrastructure..."
docker-compose up -d
echo "Waiting 10 seconds for PostgreSQL and Redis to be ready..."
sleep 10

echo "Step 2: Building shared-lib..."
mvn install -pl shared-lib -q

echo "Step 3: Starting services in dependency order..."
echo "Start these manually in separate terminals in this order:"
echo "  1. auth-service         → cd auth-service && mvn spring-boot:run"
echo "  2. customer-service     → cd customer-service && mvn spring-boot:run"
echo "  3. technician-service   → cd technician-service && mvn spring-boot:run"
echo "  4. parts-service        → cd parts-service && mvn spring-boot:run"
echo "  5. job-service          → cd job-service && mvn spring-boot:run"
echo "  6. payment-service      → cd payment-service && mvn spring-boot:run"
echo "  7. notification-service → cd notification-service && mvn spring-boot:run"
echo "  8. analytics-service    → cd analytics-service && mvn spring-boot:run"
echo "  9. api-gateway          → cd api-gateway && mvn spring-boot:run"
echo " 10. frontend             → cd frontend && npm run dev"
echo ""
echo "Ports: gateway=8080 auth=8081 jobs=8082 parts=8083"
echo "       customers=8084 technicians=8085 payments=8087 analytics=8088"
echo "       frontend=5173 kafka-ui=8090"
