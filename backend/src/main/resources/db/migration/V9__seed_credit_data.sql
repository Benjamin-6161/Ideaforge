-- Production seed: credit costs and placeholder credit packages.
-- Costs are editable at runtime (cached for 60s) and never in code.

INSERT INTO credit_costs (action, cost_units, is_active) VALUES
    ('GENERATE_IDEAS', 20, true),
    ('WORK_MESSAGE', 2, true),
    ('FINALIZE', 10, true),
    ('REVALIDATE', 5, true);

INSERT INTO credit_packages (code, name, credits_units, price_cents, currency, is_active, sort_order) VALUES
    ('starter', 'Starter', 200, 500, 'USD', true, 1),
    ('builder', 'Builder', 600, 1200, 'USD', true, 2),
    ('pro', 'Pro', 1500, 2500, 'USD', true, 3),
    ('studio', 'Studio', 4000, 5900, 'USD', true, 4);
