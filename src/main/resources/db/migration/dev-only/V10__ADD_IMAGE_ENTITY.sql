-- V1__create_image_table.sql
CREATE TABLE images (
                        id BIGSERIAL PRIMARY KEY,
                        created_at TIMESTAMP DEFAULT NOW(),
                        updated_at TIMESTAMP DEFAULT NOW(),
                        deleted BOOLEAN DEFAULT FALSE,
                        url VARCHAR(255) NOT NULL,
                        monthly_fee_id BIGINT,
                        CONSTRAINT fk_monthly_fee FOREIGN KEY (monthly_fee_id) REFERENCES monthly_fee(id) ON DELETE CASCADE
);
