CREATE INDEX idx_sale_start ON flash_sales(start_time, status);
CREATE INDEX idx_sale_end ON flash_sales(end_time, status);