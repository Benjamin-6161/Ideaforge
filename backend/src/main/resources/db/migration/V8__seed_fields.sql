-- V8__seed_fields.sql
-- Seeds the top-level industry fields and their sub-niche children used by idea generation.
-- Production seed data (not dev-only); safe to apply to any environment.

-- Top-level fields (32)
INSERT INTO fields (slug, name, description, icon, sort_order) VALUES
    ('healthcare-clinics', 'Healthcare & Clinics', 'Clinics, private practices, and care teams delivering patient services.', 'stethoscope', 1),
    ('mental-health-wellness', 'Mental Health & Wellness', 'Therapy, wellbeing, and self-care products for mental health support.', 'brain', 2),
    ('legal-services', 'Legal Services', 'Law firms and legal teams handling cases, contracts, and client matters.', 'scale', 3),
    ('accounting-tax', 'Accounting & Tax', 'Bookkeeping, tax preparation, payroll, and accounting firms.', 'calculator', 4),
    ('finance-fintech', 'Finance & Fintech', 'Banking, payments, lending, and investing software and services.', 'landmark', 5),
    ('insurance', 'Insurance', 'Insurers, brokers, and claims teams managing policies and risk.', 'shield', 6),
    ('real-estate-property-management', 'Real Estate & Property Management', 'Agents, landlords, and property managers handling listings and tenants.', 'home', 7),
    ('construction-trades', 'Construction & Trades', 'Builders, contractors, and trade crews running projects and jobs.', 'hard-hat', 8),
    ('home-services', 'Home Services', 'Local service pros handling cleaning, repair, and outdoor work.', 'wrench', 9),
    ('restaurants-food-service', 'Restaurants & Food Service', 'Restaurants, cafes, and food businesses managing service and orders.', 'utensils', 10),
    ('retail-local-small-business', 'Retail & Local Small Business', 'Shops and local merchants selling goods and serving regulars.', 'store', 11),
    ('ecommerce-dtc', 'E-commerce & DTC', 'Online stores and direct-to-consumer brands selling at scale.', 'shopping-bag', 12),
    ('hospitality-travel', 'Hospitality & Travel', 'Hotels, tours, and travel platforms serving guests and trips.', 'plane', 13),
    ('education-training', 'Education & Training', 'Schools, tutors, and training providers delivering learning.', 'graduation-cap', 14),
    ('childcare-family', 'Childcare & Family', 'Daycares, sitters, and family services supporting parents and kids.', 'baby', 15),
    ('hr-recruiting', 'HR & Recruiting', 'People teams hiring, onboarding, and supporting employees.', 'users', 16),
    ('marketing-agencies', 'Marketing & Agencies', 'Agencies and marketers running campaigns, content, and growth.', 'megaphone', 17),
    ('creators-media', 'Creators & Media', 'Creators, publishers, and media teams producing and monetizing content.', 'clapperboard', 18),
    ('software-dev-devops', 'Software Dev & DevOps', 'Engineering teams building, shipping, and operating software.', 'code', 19),
    ('cybersecurity-compliance', 'Cybersecurity & Compliance', 'Security teams protecting systems and meeting regulatory rules.', 'lock', 20),
    ('logistics-supply-chain', 'Logistics & Supply Chain', 'Freight, warehousing, and delivery operations moving goods.', 'truck', 21),
    ('manufacturing', 'Manufacturing', 'Factories and production teams building and shipping physical goods.', 'factory', 22),
    ('agriculture-food-production', 'Agriculture & Food Production', 'Farms, growers, and food producers managing crops and supply.', 'sprout', 23),
    ('energy-sustainability', 'Energy & Sustainability', 'Clean energy, efficiency, and sustainability-focused businesses.', 'leaf', 24),
    ('automotive-fleet', 'Automotive & Fleet', 'Repair shops, dealers, and fleet operators managing vehicles.', 'car', 25),
    ('fitness-sports', 'Fitness & Sports', 'Gyms, trainers, clubs, and sports programs keeping people active.', 'dumbbell', 26),
    ('beauty-personal-care', 'Beauty & Personal Care', 'Salons, spas, and personal care brands serving clients.', 'sparkles', 27),
    ('nonprofits-community', 'Nonprofits & Community', 'Charities and community groups managing donors and programs.', 'heart-handshake', 28),
    ('government-civic-tech', 'Government & Civic Tech', 'Public agencies and civic platforms delivering services to residents.', 'landmark', 29),
    ('events-entertainment', 'Events & Entertainment', 'Event organizers, venues, and entertainment businesses.', 'ticket', 30),
    ('freelancers-solo-professionals', 'Freelancers & Solo Professionals', 'Independent professionals running their own practice and clients.', 'laptop', 31),
    ('veterinary-pet-care', 'Veterinary & Pet Care', 'Vets, groomers, and pet businesses caring for animals.', 'paw-print', 32);

-- Sub-niches: Healthcare & Clinics
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('healthcare-clinics-medical-billing', 'Medical Billing', 'Billing, coding, and revenue cycle tools for clinics.', 'receipt', 1),
    ('healthcare-clinics-telehealth', 'Telehealth', 'Virtual visits, remote consults, and connected care delivery.', 'video', 2),
    ('healthcare-clinics-dental-practices', 'Dental Practices', 'Practice software and services for dental offices.', 'smile', 3),
    ('healthcare-clinics-patient-scheduling', 'Patient Scheduling', 'Appointment booking and reminders for care providers.', 'calendar-check', 4),
    ('healthcare-clinics-physical-therapy', 'Physical Therapy', 'Rehab, recovery, and movement-focused treatment services.', 'activity', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'healthcare-clinics';

-- Sub-niches: Mental Health & Wellness
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('mental-health-wellness-therapy-practices', 'Therapy Practices', 'Software and services for counselors and therapists.', 'message-circle', 1),
    ('mental-health-wellness-meditation-apps', 'Meditation Apps', 'Guided meditation, breathing, and mindfulness products.', 'moon', 2),
    ('mental-health-wellness-habit-tracking', 'Habit Tracking', 'Tools that help people build and keep healthy routines.', 'check-circle', 3),
    ('mental-health-wellness-crisis-support', 'Crisis Support', 'Hotlines, peer support, and urgent mental health resources.', 'life-buoy', 4),
    ('mental-health-wellness-corporate-wellbeing', 'Corporate Wellbeing', 'Employee mental health and wellness programs at work.', 'briefcase', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'mental-health-wellness';

-- Sub-niches: Legal Services
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('legal-services-law-firm-intake', 'Law Firm Intake', 'Client intake, screening, and matter opening for firms.', 'inbox', 1),
    ('legal-services-contract-review', 'Contract Review', 'Drafting, reviewing, and managing legal agreements.', 'file-text', 2),
    ('legal-services-immigration', 'Immigration', 'Visa, residency, and immigration case support.', 'globe', 3),
    ('legal-services-family-law', 'Family Law', 'Divorce, custody, and family legal matter services.', 'users', 4),
    ('legal-services-legal-billing', 'Legal Billing', 'Timekeeping and billing tools for legal practices.', 'receipt', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'legal-services';

-- Sub-niches: Accounting & Tax
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('accounting-tax-bookkeeping', 'Bookkeeping', 'Day-to-day bookkeeping and reconciliation for small business.', 'book-open', 1),
    ('accounting-tax-tax-prep', 'Tax Prep', 'Tax filing, planning, and compliance for individuals and firms.', 'calculator', 2),
    ('accounting-tax-payroll', 'Payroll', 'Payroll runs, filings, and worker payments.', 'wallet', 3),
    ('accounting-tax-audit-support', 'Audit Support', 'Preparation and evidence management for audits.', 'search-check', 4),
    ('accounting-tax-cpa-practices', 'CPA Practices', 'Operations and client tools for accounting firms.', 'briefcase', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'accounting-tax';

-- Sub-niches: Finance & Fintech
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('finance-fintech-personal-finance', 'Personal Finance', 'Budgeting, saving, and money management apps.', 'piggy-bank', 1),
    ('finance-fintech-lending', 'Lending', 'Loan origination, underwriting, and servicing platforms.', 'hand-coins', 2),
    ('finance-fintech-payments', 'Payments', 'Payment processing, transfers, and settlement tools.', 'credit-card', 3),
    ('finance-fintech-investing', 'Investing', 'Portfolio, trading, and wealth management products.', 'trending-up', 4),
    ('finance-fintech-credit-scoring', 'Credit Scoring', 'Credit assessment, monitoring, and reporting services.', 'gauge', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'finance-fintech';

-- Sub-niches: Insurance
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('insurance-agencies', 'Agencies', 'Tools for independent agents and brokerages.', 'building', 1),
    ('insurance-claims-processing', 'Claims Processing', 'Claims intake, adjudication, and payout workflows.', 'clipboard-check', 2),
    ('insurance-underwriting', 'Underwriting', 'Risk evaluation and policy pricing support.', 'shield-check', 3),
    ('insurance-comparison', 'Comparison', 'Quote comparison and policy shopping platforms.', 'search', 4),
    ('insurance-risk-assessment', 'Risk Assessment', 'Risk modeling, scoring, and mitigation tools.', 'alert-triangle', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'insurance';

-- Sub-niches: Real Estate & Property Management
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('real-estate-property-management-listings', 'Listings', 'Property listing, marketing, and lead management.', 'building-2', 1),
    ('real-estate-property-management-rent-collection', 'Rent Collection', 'Rent payments, reminders, and receipts for landlords.', 'banknote', 2),
    ('real-estate-property-management-tenant-screening', 'Tenant Screening', 'Background, credit, and reference checks for tenants.', 'user-check', 3),
    ('real-estate-property-management-maintenance', 'Maintenance', 'Work orders and vendor coordination for properties.', 'hammer', 4),
    ('real-estate-property-management-valuation', 'Valuation', 'Pricing, appraisal, and market analysis tools.', 'trending-up', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'real-estate-property-management';

-- Sub-niches: Construction & Trades
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('construction-trades-estimating', 'Estimating', 'Job takeoffs, quotes, and bid management for crews.', 'ruler', 1),
    ('construction-trades-project-management', 'Project Management', 'Schedules, tasks, and site coordination for builds.', 'clipboard-list', 2),
    ('construction-trades-electrical', 'Electrical', 'Electrical contracting and service business tools.', 'zap', 3),
    ('construction-trades-plumbing', 'Plumbing', 'Plumbing contractors and service call operations.', 'droplet', 4),
    ('construction-trades-safety-compliance', 'Safety Compliance', 'Site safety, inspections, and regulatory compliance.', 'shield-check', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'construction-trades';

-- Sub-niches: Home Services
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('home-services-cleaning', 'Cleaning', 'Residential and commercial cleaning business tools.', 'spray-can', 1),
    ('home-services-landscaping', 'Landscaping', 'Lawn, garden, and outdoor maintenance services.', 'tree-pine', 2),
    ('home-services-pest-control', 'Pest Control', 'Extermination, inspection, and treatment scheduling.', 'bug', 3),
    ('home-services-handyman', 'Handyman', 'Small repair and odd-job service businesses.', 'drill', 4),
    ('home-services-hvac', 'HVAC', 'Heating, cooling, and ventilation service operations.', 'fan', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'home-services';

-- Sub-niches: Restaurants & Food Service
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('restaurants-food-service-pos', 'POS', 'Point of sale, ordering, and payment for food venues.', 'receipt', 1),
    ('restaurants-food-service-inventory', 'Inventory', 'Stock, waste, and supplier tracking for kitchens.', 'boxes', 2),
    ('restaurants-food-service-reservations', 'Reservations', 'Table booking and waitlist management for venues.', 'calendar-clock', 3),
    ('restaurants-food-service-delivery', 'Delivery', 'Delivery, takeout, and courier coordination.', 'bike', 4),
    ('restaurants-food-service-staff-scheduling', 'Staff Scheduling', 'Shift planning and labor management for food teams.', 'calendar-days', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'restaurants-food-service';

-- Sub-niches: Retail & Local Small Business
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('retail-local-small-business-inventory', 'Inventory', 'Stock counts, purchasing, and product catalogs for shops.', 'package', 1),
    ('retail-local-small-business-loyalty', 'Loyalty', 'Rewards and repeat-customer programs for merchants.', 'gift', 2),
    ('retail-local-small-business-payments', 'Payments', 'In-store payments, terminals, and cash flow tools.', 'credit-card', 3),
    ('retail-local-small-business-customer-insights', 'Customer Insights', 'Sales analytics and shopper behavior for local stores.', 'bar-chart', 4),
    ('retail-local-small-business-marketplace', 'Marketplace', 'Local discovery and online storefront platforms.', 'store', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'retail-local-small-business';

-- Sub-niches: E-commerce & DTC
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('ecommerce-dtc-shipping', 'Shipping', 'Fulfillment, rates, and delivery tracking for online stores.', 'package-check', 1),
    ('ecommerce-dtc-returns', 'Returns', 'Return handling, exchanges, and refund workflows.', 'rotate-ccw', 2),
    ('ecommerce-dtc-subscriptions', 'Subscriptions', 'Recurring billing and subscription box products.', 'repeat', 3),
    ('ecommerce-dtc-conversion-optimization', 'Conversion Optimization', 'Storefront testing, personalization, and checkout tools.', 'trending-up', 4),
    ('ecommerce-dtc-product-sourcing', 'Product Sourcing', 'Supplier discovery and merchandising for brands.', 'globe', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'ecommerce-dtc';

-- Sub-niches: Hospitality & Travel
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('hospitality-travel-booking', 'Booking', 'Reservations and booking engines for stays and trips.', 'calendar-check', 1),
    ('hospitality-travel-hotels', 'Hotels', 'Front desk, housekeeping, and guest experience tools.', 'bed-double', 2),
    ('hospitality-travel-tours', 'Tours', 'Guided tours, activities, and experience operators.', 'map', 3),
    ('hospitality-travel-trip-planning', 'Trip Planning', 'Itineraries, logistics, and travel planning assistants.', 'route', 4),
    ('hospitality-travel-short-term-rentals', 'Short-Term Rentals', 'Vacation rental pricing, turnover, and guest messaging.', 'key', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'hospitality-travel';

-- Sub-niches: Education & Training
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('education-training-tutoring', 'Tutoring', 'Matching, scheduling, and billing for tutors.', 'pencil', 1),
    ('education-training-lms', 'LMS', 'Course delivery and learner management platforms.', 'school', 2),
    ('education-training-test-prep', 'Test Prep', 'Exam preparation and practice assessment products.', 'clipboard-check', 3),
    ('education-training-corporate-training', 'Corporate Training', 'Workplace learning and skills development programs.', 'presentation', 4),
    ('education-training-language-learning', 'Language Learning', 'Language courses, practice, and fluency tools.', 'languages', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'education-training';

-- Sub-niches: Childcare & Family
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('childcare-family-daycares', 'Daycares', 'Enrollment, ratios, and daily reporting for daycare centers.', 'baby', 1),
    ('childcare-family-babysitting', 'Babysitting', 'Sitter matching, vetting, and booking platforms.', 'heart', 2),
    ('childcare-family-parenting', 'Parenting', 'Apps and services supporting parents day to day.', 'users', 3),
    ('childcare-family-family-scheduling', 'Family Scheduling', 'Shared calendars and coordination for busy families.', 'calendar', 4),
    ('childcare-family-early-learning', 'Early Learning', 'Preschool curriculum and early childhood development.', 'puzzle', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'childcare-family';

-- Sub-niches: HR & Recruiting
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('hr-recruiting-applicant-tracking', 'Applicant Tracking', 'Job postings, pipelines, and candidate management.', 'user-search', 1),
    ('hr-recruiting-onboarding', 'Onboarding', 'New hire setup, checklists, and first-day workflows.', 'clipboard-list', 2),
    ('hr-recruiting-employee-engagement', 'Employee Engagement', 'Surveys, feedback, and retention for teams.', 'smile', 3),
    ('hr-recruiting-payroll-benefits', 'Payroll Benefits', 'Compensation, benefits, and total rewards management.', 'wallet', 4),
    ('hr-recruiting-talent-sourcing', 'Talent Sourcing', 'Proactive candidate search and outreach tooling.', 'target', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'hr-recruiting';

-- Sub-niches: Marketing & Agencies
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('marketing-agencies-campaign-management', 'Campaign Management', 'Planning, execution, and reporting for campaigns.', 'send', 1),
    ('marketing-agencies-seo', 'SEO', 'Search visibility, audits, and content optimization.', 'search', 2),
    ('marketing-agencies-social-media', 'Social Media', 'Scheduling, publishing, and community management.', 'share-2', 3),
    ('marketing-agencies-influencer', 'Influencer', 'Creator partnerships, briefs, and campaign tracking.', 'star', 4),
    ('marketing-agencies-analytics', 'Analytics', 'Attribution, dashboards, and growth measurement.', 'trending-up', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'marketing-agencies';

-- Sub-niches: Creators & Media
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('creators-media-newsletters', 'Newsletters', 'Writing, distribution, and monetization for newsletters.', 'mail', 1),
    ('creators-media-video-production', 'Video Production', 'Filming, editing, and publishing workflows for video.', 'video', 2),
    ('creators-media-podcasting', 'Podcasting', 'Recording, hosting, and growth tools for podcasts.', 'mic', 3),
    ('creators-media-memberships', 'Memberships', 'Paid communities and subscriber perks for creators.', 'badge-check', 4),
    ('creators-media-content-repurposing', 'Content Repurposing', 'Turning one piece of content into many formats.', 'repeat', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'creators-media';

-- Sub-niches: Software Dev & DevOps
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('software-dev-devops-code-review', 'Code Review', 'Review workflows, diffs, and quality tooling.', 'git-pull-request', 1),
    ('software-dev-devops-ci-cd', 'CI/CD', 'Build, test, and deployment pipelines for teams.', 'rocket', 2),
    ('software-dev-devops-observability', 'Observability', 'Logs, metrics, traces, and incident response.', 'activity', 3),
    ('software-dev-devops-api-tooling', 'API Tooling', 'Design, testing, and documentation for APIs.', 'plug', 4),
    ('software-dev-devops-developer-onboarding', 'Developer Onboarding', 'Environment setup and ramp-up for new engineers.', 'terminal', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'software-dev-devops';

-- Sub-niches: Cybersecurity & Compliance
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('cybersecurity-compliance-soc', 'SOC', 'Security monitoring, alerting, and response operations.', 'monitor', 1),
    ('cybersecurity-compliance-gdpr', 'GDPR', 'Privacy compliance, consent, and data subject requests.', 'shield-check', 2),
    ('cybersecurity-compliance-penetration-testing', 'Penetration Testing', 'Offensive testing, scoping, and remediation reports.', 'bug', 3),
    ('cybersecurity-compliance-identity-access', 'Identity Access', 'SSO, permissions, and access governance.', 'key-round', 4),
    ('cybersecurity-compliance-vulnerability-management', 'Vulnerability Management', 'Scanning, prioritization, and patching workflows.', 'alert-triangle', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'cybersecurity-compliance';

-- Sub-niches: Logistics & Supply Chain
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('logistics-supply-chain-fleet-tracking', 'Fleet Tracking', 'Vehicle location, telematics, and dispatch visibility.', 'map-pin', 1),
    ('logistics-supply-chain-warehousing', 'Warehousing', 'Storage, picking, and warehouse operations management.', 'warehouse', 2),
    ('logistics-supply-chain-last-mile', 'Last Mile', 'Final delivery routing and proof of delivery.', 'map', 3),
    ('logistics-supply-chain-procurement', 'Procurement', 'Purchasing, vendors, and spend management.', 'shopping-cart', 4),
    ('logistics-supply-chain-route-optimization', 'Route Optimization', 'Planning efficient routes and delivery schedules.', 'route', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'logistics-supply-chain';

-- Sub-niches: Manufacturing
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('manufacturing-production-planning', 'Production Planning', 'Scheduling, capacity, and work order planning.', 'cog', 1),
    ('manufacturing-quality-control', 'Quality Control', 'Inspections, defects, and quality assurance tracking.', 'badge-check', 2),
    ('manufacturing-equipment-maintenance', 'Equipment Maintenance', 'Preventive maintenance and asset uptime management.', 'wrench', 3),
    ('manufacturing-supplier-management', 'Supplier Management', 'Vendor relationships and incoming material quality.', 'handshake', 4),
    ('manufacturing-shop-floor-analytics', 'Shop Floor Analytics', 'Throughput, OEE, and machine performance insights.', 'gauge', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'manufacturing';

-- Sub-niches: Agriculture & Food Production
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('agriculture-food-production-farm-management', 'Farm Management', 'Field records, planning, and farm operations tools.', 'tractor', 1),
    ('agriculture-food-production-crop-monitoring', 'Crop Monitoring', 'Sensors, imagery, and crop health tracking.', 'leaf', 2),
    ('agriculture-food-production-livestock', 'Livestock', 'Herd tracking, health, and breeding management.', 'beef', 3),
    ('agriculture-food-production-food-processing', 'Food Processing', 'Processing lines, packaging, and food safety.', 'factory', 4),
    ('agriculture-food-production-local-food', 'Local Food', 'Farm-to-table sales, CSAs, and local markets.', 'carrot', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'agriculture-food-production';

-- Sub-niches: Energy & Sustainability
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('energy-sustainability-solar', 'Solar', 'Solar installation, monitoring, and financing.', 'sun', 1),
    ('energy-sustainability-carbon-tracking', 'Carbon Tracking', 'Emissions measurement, reporting, and reduction.', 'cloud', 2),
    ('energy-sustainability-ev-charging', 'EV Charging', 'Charging networks, stations, and driver services.', 'plug-zap', 3),
    ('energy-sustainability-waste-reduction', 'Waste Reduction', 'Recycling, composting, and waste logistics.', 'trash-2', 4),
    ('energy-sustainability-energy-audits', 'Energy Audits', 'Efficiency assessments and retrofit planning.', 'gauge', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'energy-sustainability';

-- Sub-niches: Automotive & Fleet
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('automotive-fleet-repair-shops', 'Repair Shops', 'Service bays, estimates, and repair order management.', 'wrench', 1),
    ('automotive-fleet-dealerships', 'Dealerships', 'Inventory, sales, and service for car dealers.', 'car', 2),
    ('automotive-fleet-fleet-maintenance', 'Fleet Maintenance', 'Schedules, inspections, and repair history for fleets.', 'truck', 3),
    ('automotive-fleet-parts', 'Parts', 'Parts catalogs, ordering, and inventory.', 'cog', 4),
    ('automotive-fleet-vehicle-inspection', 'Vehicle Inspection', 'Inspections, checklists, and compliance reporting.', 'clipboard-check', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'automotive-fleet';

-- Sub-niches: Fitness & Sports
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('fitness-sports-gyms', 'Gyms', 'Membership, access, and gym operations management.', 'dumbbell', 1),
    ('fitness-sports-personal-training', 'Personal Training', 'Client programs, progress, and coaching tools.', 'user-check', 2),
    ('fitness-sports-team-management', 'Team Management', 'Rosters, schedules, and communication for teams.', 'users', 3),
    ('fitness-sports-class-booking', 'Class Booking', 'Schedules and bookings for fitness classes.', 'calendar-check', 4),
    ('fitness-sports-nutrition', 'Nutrition', 'Meal plans and nutrition coaching for athletes.', 'apple', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'fitness-sports';

-- Sub-niches: Beauty & Personal Care
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('beauty-personal-care-salons', 'Salons', 'Booking, stylists, and retail for hair and nail salons.', 'scissors', 1),
    ('beauty-personal-care-spas', 'Spas', 'Treatments, rooms, and guest experience for spas.', 'flower', 2),
    ('beauty-personal-care-barbershops', 'Barbershops', 'Chair scheduling, payments, and shop management.', 'scissors', 3),
    ('beauty-personal-care-appointment-booking', 'Appointment Booking', 'Scheduling and reminders for personal care pros.', 'calendar-check', 4),
    ('beauty-personal-care-product-recommendations', 'Product Recommendations', 'Personalized product matching and routines.', 'heart', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'beauty-personal-care';

-- Sub-niches: Nonprofits & Community
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('nonprofits-community-donor-management', 'Donor Management', 'Donor records, gifts, and stewardship tools.', 'gift', 1),
    ('nonprofits-community-volunteer-coordination', 'Volunteer Coordination', 'Shifts, signups, and recognition for volunteers.', 'hand-heart', 2),
    ('nonprofits-community-grant-writing', 'Grant Writing', 'Grant discovery, applications, and reporting.', 'file-text', 3),
    ('nonprofits-community-fundraising', 'Fundraising', 'Campaigns, events, and donation collection.', 'coins', 4),
    ('nonprofits-community-member-engagement', 'Member Engagement', 'Community programs and member communication.', 'users', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'nonprofits-community';

-- Sub-niches: Government & Civic Tech
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('government-civic-tech-permitting', 'Permitting', 'License, permit, and inspection workflows for agencies.', 'file-check', 1),
    ('government-civic-tech-public-transparency', 'Public Transparency', 'Open data, records, and accountability platforms.', 'eye', 2),
    ('government-civic-tech-citizen-services', 'Citizen Services', 'Service requests and resident support portals.', 'building-2', 3),
    ('government-civic-tech-voting', 'Voting', 'Registration, ballots, and election information.', 'vote', 4),
    ('government-civic-tech-community-planning', 'Community Planning', 'Zoning, mapping, and public planning tools.', 'map', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'government-civic-tech';

-- Sub-niches: Events & Entertainment
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('events-entertainment-ticketing', 'Ticketing', 'Ticket sales, check-in, and attendee management.', 'ticket', 1),
    ('events-entertainment-venue-management', 'Venue Management', 'Bookings, capacity, and operations for venues.', 'building', 2),
    ('events-entertainment-event-planning', 'Event Planning', 'Planning, vendors, and timelines for events.', 'calendar', 3),
    ('events-entertainment-artists', 'Artists', 'Booking and promotion tools for performers.', 'music', 4),
    ('events-entertainment-streaming', 'Streaming', 'Live and on-demand entertainment streaming.', 'radio', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'events-entertainment';

-- Sub-niches: Freelancers & Solo Professionals
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('freelancers-solo-professionals-invoicing', 'Invoicing', 'Estimates, invoices, and payment reminders.', 'file-text', 1),
    ('freelancers-solo-professionals-portfolio', 'Portfolio', 'Showcasing work and winning new clients.', 'image', 2),
    ('freelancers-solo-professionals-contracts', 'Contracts', 'Proposals, agreements, and e-signatures.', 'file-signature', 3),
    ('freelancers-solo-professionals-time-tracking', 'Time Tracking', 'Billable hours and project time management.', 'clock', 4),
    ('freelancers-solo-professionals-client-crm', 'Client CRM', 'Pipeline, contacts, and client communication.', 'users', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'freelancers-solo-professionals';

-- Sub-niches: Veterinary & Pet Care
INSERT INTO fields (parent_id, slug, name, description, icon, sort_order)
SELECT p.id, v.slug, v.name, v.description, v.icon, v.sort_order
FROM (VALUES
    ('veterinary-pet-care-vet-clinics', 'Vet Clinics', 'Records, appointments, and billing for vet practices.', 'stethoscope', 1),
    ('veterinary-pet-care-pet-grooming', 'Pet Grooming', 'Grooming appointments and client management.', 'scissors', 2),
    ('veterinary-pet-care-boarding', 'Boarding', 'Kennels, sitting, and pet stay management.', 'home', 3),
    ('veterinary-pet-care-pet-insurance', 'Pet Insurance', 'Coverage, claims, and wellness plans for pets.', 'shield', 4),
    ('veterinary-pet-care-pet-training', 'Pet Training', 'Obedience classes and behavior coaching.', 'award', 5)
) AS v(slug, name, description, icon, sort_order)
JOIN fields p ON p.slug = 'veterinary-pet-care';

-- Production seed data (not dev-only): required in all environments for idea generation.
