-- Add charge breakdown columns for dynamic service charge logic
ALTER TABLE jobs
    ADD COLUMN IF NOT EXISTS base_charge       DECIMAL(10,2),
    ADD COLUMN IF NOT EXISTS travel_surcharge  DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS urgency_surcharge DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS discount          DECIMAL(10,2) DEFAULT 0,
    ADD COLUMN IF NOT EXISTS final_charge      DECIMAL(10,2);

COMMENT ON COLUMN jobs.base_charge       IS 'Minimum service charge from config: booking.service_charge_minimum';
COMMENT ON COLUMN jobs.travel_surcharge  IS 'Extra charge for non-city areas from config: booking.travel_extra_*';
COMMENT ON COLUMN jobs.urgency_surcharge IS 'Admin-set surcharge for urgent/priority jobs';
COMMENT ON COLUMN jobs.discount          IS 'Admin-set discount amount';
COMMENT ON COLUMN jobs.final_charge      IS 'Auto-computed: base + travel + urgency + parts + labor - discount';
