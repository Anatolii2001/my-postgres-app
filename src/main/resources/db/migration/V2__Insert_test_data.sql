-- V2__Insert_test_data.sql
-- Вставка тестовых данных для демонстрации

INSERT INTO order_status ("имя статуса") VALUES ('В обработке'), ('Отправлен'), ('Доставлен'), ('Отменен');
INSERT INTO product (описание, стоимость, количество, категория) VALUES ('Ноутбук', 50000.00, 10, 'Электроника');
INSERT INTO customer (имя, фамилия, email) VALUES ('Иван', 'Иванов', 'ivan@example.com');
