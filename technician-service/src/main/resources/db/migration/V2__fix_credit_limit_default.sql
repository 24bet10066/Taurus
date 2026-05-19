-- Fix: new technicians start with credit_limit = 0 (admin must explicitly enable credit)
ALTER TABLE technicians ALTER COLUMN credit_limit SET DEFAULT 0;
