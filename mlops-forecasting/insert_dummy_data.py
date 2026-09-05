import psycopg2
from datetime import datetime, timedelta
import random

try:
    conn = psycopg2.connect("postgresql://postgres:postgres@localhost:5432/serviceos_jobs")
    cur = conn.cursor()
    
    # Generate 60 days of data
    start_date = datetime.now() - timedelta(days=60)
    for i in range(60):
        current_date = start_date + timedelta(days=i)
        
        # Base demand + random variation + weekly seasonality
        base = 10
        if current_date.weekday() >= 5: # Weekend, slightly more demand
            base = 15
            
        daily_demand = base + random.randint(-5, 5)
        
        # Insert records for this day (1 part used per record to total daily_demand)
        for _ in range(daily_demand):
            # Using random job_id for dummy records
            job_id = "538e1b01-8fcb-4c22-9022-dfa6b633d9c1"
            part_id = "11111111-1111-1111-1111-111111111111"
            quantity = 1
            unit_cost = 50.00
            source = "SHOP"
            added_at = current_date.strftime('%Y-%m-%d %H:%M:%S')
            
            cur.execute("""
                INSERT INTO job_parts_used (id, job_id, part_id, quantity, unit_cost, source, added_at)
                VALUES (gen_random_uuid(), %s, %s, %s, %s, %s, %s)
            """, (job_id, part_id, quantity, unit_cost, source, added_at))
            
    conn.commit()
    print("Successfully inserted 60 days of dummy parts demand data.")
    cur.close()
    conn.close()
except Exception as e:
    print(f"Error: {e}")
