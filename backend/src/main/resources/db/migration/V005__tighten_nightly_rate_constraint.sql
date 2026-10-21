-- Enforce the project rule: nightly rate must be positive with at most two decimal places.
-- Replaces the permissive >= 0 CHECK that allowed zero and four-decimal rates.

ALTER TABLE properties DROP CONSTRAINT properties_base_nightly_rate_check;
ALTER TABLE properties ADD CONSTRAINT properties_base_nightly_rate_check
    CHECK (base_nightly_rate > 0 AND base_nightly_rate * 100 = TRUNC(base_nightly_rate * 100));
