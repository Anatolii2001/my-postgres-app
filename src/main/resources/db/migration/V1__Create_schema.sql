-- V1__Create_schema.sql
-- Схема базы данных

CREATE TABLE public.customer (
                                 id SERIAL PRIMARY KEY NOT NULL,
                                 имя CHARACTER VARYING(50) NOT NULL,
                                 фамилия CHARACTER VARYING(50) NOT NULL,
                                 телефон CHARACTER VARYING(20),
                                 email CHARACTER VARYING(100) NOT NULL
);
CREATE UNIQUE INDEX customer_email_key ON customer USING BTREE (email);

CREATE TABLE public.product (
                                id SERIAL PRIMARY KEY NOT NULL,
                                описание TEXT NOT NULL,
                                стоимость NUMERIC(10,2) NOT NULL,
                                количество INTEGER NOT NULL,
                                категория CHARACTER VARYING(100) NOT NULL
);

CREATE TABLE public.order_status (
                                     id SERIAL PRIMARY KEY NOT NULL,
                                     "имя статуса" CHARACTER VARYING(50) NOT NULL
);
CREATE UNIQUE INDEX order_status_status_name_key ON order_status USING BTREE ("имя статуса");

CREATE TABLE public."order" (
                                id SERIAL PRIMARY KEY NOT NULL,
                                product_id INTEGER NOT NULL,
                                customer_id INTEGER NOT NULL,
                                "дата заказа" DATE NOT NULL,
                                количество INTEGER NOT NULL,
                                статус INTEGER NOT NULL,
                                FOREIGN KEY (customer_id) REFERENCES public.customer (id),
                                FOREIGN KEY (product_id) REFERENCES public.product (id),
                                FOREIGN KEY (статус) REFERENCES public.order_status (id)
);
CREATE INDEX idx_order_product_id ON "order" USING BTREE (product_id);
CREATE INDEX idx_order_customer_id ON "order" USING BTREE (customer_id);
CREATE INDEX idx_order_status ON "order" USING BTREE (статус);
CREATE INDEX idx_order_date ON "order" USING BTREE ("дата заказа");
