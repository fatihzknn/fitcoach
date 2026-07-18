-- Track which formula was used to compute body_fat_percentage:
--   NAVY  = US Navy circumference method (men: waist-neck-height)
--   BAI   = Body Adiposity Index        (women: hip-height, more accurate for women)
ALTER TABLE body_measurements
    ADD COLUMN body_fat_method VARCHAR(10);

-- Back-fill: every existing row was computed with the Navy method
UPDATE body_measurements
SET body_fat_method = 'NAVY'
WHERE body_fat_percentage IS NOT NULL;
