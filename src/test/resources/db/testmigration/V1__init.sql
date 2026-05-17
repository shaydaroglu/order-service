CREATE TABLE IF NOT EXISTS orders
(
    id                UUID                        NOT NULL DEFAULT random_uuid(),
    state             VARCHAR(50)                 NOT NULL,
    category          VARCHAR(10)                 NOT NULL,
    customer_id       VARCHAR(255)                NOT NULL,
    site_id           VARCHAR(255)                NOT NULL,
    payment_type      VARCHAR(50)                 NOT NULL,
    iban              VARCHAR(34),
    idempotency_key   VARCHAR(255)                UNIQUE,
    version           BIGINT                      NOT NULL DEFAULT 0,
    created_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMP WITH TIME ZONE    NOT NULL DEFAULT now(),
    CONSTRAINT pk_orders PRIMARY KEY (id)
);

CREATE TABLE IF NOT EXISTS order_items
(
    id                    UUID            NOT NULL DEFAULT random_uuid(),
    order_id              UUID            NOT NULL,
    product_offering_id   UUID            NOT NULL,
    quantity              INTEGER         NOT NULL CHECK (quantity >= 1),
    CONSTRAINT pk_order_items PRIMARY KEY (id),
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders (id) ON DELETE CASCADE
);
