-- Run manually in PostgreSQL/pgAdmin for the inventory service schema.
-- Hibernate validation expects this table when the service starts.

CREATE TABLE IF NOT EXISTS inventory_schema.reservations (
    reservation_id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    order_id UUID NULL,
    status VARCHAR(20) NOT NULL CHECK (
        status IN ('ACTIVE', 'RELEASED', 'CONFIRMED')
    ),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reservations_product_id
    ON inventory_schema.reservations (product_id);

CREATE INDEX IF NOT EXISTS idx_reservations_order_id
    ON inventory_schema.reservations (order_id);
