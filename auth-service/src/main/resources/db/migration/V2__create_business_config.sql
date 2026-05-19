CREATE TABLE IF NOT EXISTS business_config (
    key         VARCHAR(100) PRIMARY KEY,
    value       TEXT         NOT NULL,
    category    VARCHAR(50),
    description VARCHAR(255),
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100)
);

-- Seed real business values for SK Electronics, Banda UP
INSERT INTO business_config (key, value, category, description) VALUES
  ('shop.name',                   'SK Electronics',                                                                                   'SHOP',        'Business trading name'),
  ('shop.owner',                  'Sushil Gupta',                                                                                     'SHOP',        'Owner name'),
  ('shop.phone',                  '8960245022',                                                                                       'SHOP',        'Primary contact / OTP fallback'),
  ('shop.whatsapp',               '8960245022',                                                                                       'SHOP',        'WhatsApp notification number'),
  ('shop.address',                'SK Electronics, Balkhandi Naka to Katra Road, near Masjid, Banda, Uttar Pradesh - 210001',         'SHOP',        'Full shop address'),
  ('shop.working_hours_start',    '10:00',                                                                                            'SHOP',        'Opening time (HH:mm, IST)'),
  ('shop.working_hours_end',      '20:00',                                                                                            'SHOP',        'Closing time (HH:mm, IST)'),
  ('shop.working_days',           'MON,TUE,WED,THU,FRI,SAT,SUN',                                                                     'SHOP',        'Comma-separated working days'),
  ('shop.city',                   'Banda',                                                                                            'SHOP',        'Primary city for area classification'),
  ('shop.state',                  'Uttar Pradesh',                                                                                    'SHOP',        'State'),
  ('shop.pincode',                '210001',                                                                                           'SHOP',        'Shop pincode'),
  ('booking.service_charge_minimum',  '300',                                                                                          'PRICING',     'Minimum visit / service charge in INR'),
  ('booking.travel_extra_village',    '100',                                                                                          'PRICING',     'Extra travel surcharge for village bookings'),
  ('booking.travel_extra_town',       '50',                                                                                           'PRICING',     'Extra travel surcharge for non-city town bookings'),
  ('notification.sms_cooldown_hours', '24',                                                                                           'NOTIFICATION','Hours between duplicate SMS to same phone+type'),
  ('notification.prefer_whatsapp',    'true',                                                                                         'NOTIFICATION','Prefer WhatsApp over SMS when both are available'),
  ('credit.new_technician_limit',     '0',                                                                                            'OPERATION',   'Default credit limit for new technicians (0 = disabled)'),
  ('credit.parts_markup_default_percent', '20',                                                                                       'PRICING',     'Default markup % applied to parts before crediting')
ON CONFLICT (key) DO NOTHING;
