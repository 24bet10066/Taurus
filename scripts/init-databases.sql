-- PostgreSQL init script — runs once on first container creation.
-- If databases already exist (stale volume), run:
--   docker-compose down -v && docker-compose up -d

CREATE DATABASE serviceos_auth;
CREATE DATABASE serviceos_jobs;
CREATE DATABASE serviceos_parts;
CREATE DATABASE serviceos_customers;
CREATE DATABASE serviceos_technicians;
CREATE DATABASE serviceos_payments;
CREATE DATABASE serviceos_analytics;
