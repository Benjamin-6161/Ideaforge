-- Hibernate's schema validation maps Java String to varchar; use varchar(3)
-- for ISO currency codes instead of bpchar.
ALTER TABLE credit_packages ALTER COLUMN currency TYPE varchar(3);
ALTER TABLE payments ALTER COLUMN currency TYPE varchar(3);
