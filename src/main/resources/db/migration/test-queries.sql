-- test-queries.sql
-- Файл с тестовыми запросами для базы данных заказов.
-- Включает 5 запросов на чтение, 3 на обновление и 2 на удаление.

-- === ЗАПРОСЫ НА ЧТЕНИЕ ===

-- 1. Список всех заказов за последние 7 дней с именем покупателя и описанием товара (JOIN с несколькими таблицами, фильтрация по дате).
SELECT o.id AS order_id,
       c.имя || ' ' || c.фамилия AS customer_name,
       p.описание AS product_description,
       o."дата заказа" AS order_date,
       o.количество AS quantity,
       os."имя статуса" AS status_name
FROM "order" o
         JOIN customer c ON o.customer_id = c.id
         JOIN product p ON o.product_id = p.id
         JOIN order_status os ON o.статус = os.id
WHERE o."дата заказа" >= CURRENT_DATE - INTERVAL '7 days'
ORDER BY o."дата заказа" DESC;

-- 2. Топ-3 самых популярных товаров по суммарному количеству заказов (агрегаты, сортировка).
SELECT p.описание AS product_description,
       SUM(o.количество) AS total_ordered
FROM "order" o
         JOIN product p ON o.product_id = p.id
GROUP BY p.описание
ORDER BY total_ordered DESC
    LIMIT 3;

-- 3. Количество клиентов без заказов (агрегат, фильтрация с LEFT JOIN).
SELECT COUNT(*) AS no_order_customers
FROM customer c
         LEFT JOIN "order" o ON c.id = o.customer_id
WHERE o.id IS NULL;

-- 4. Средняя стоимость товаров по категориям с сортировкой по средней цене (агрегаты, группировка, сортировка).
SELECT p.категория AS category,
       AVG(p.стоимость) AS avg_price,
       COUNT(*) AS product_count
FROM product p
GROUP BY p.категория
ORDER BY avg_price DESC;

-- 5. Заказы с фильтрацией по статусу "В обработке" и суммой заказа больше 10000 (JOIN, фильтрация, вычисление).
SELECT o.id AS order_id,
       c.имя || ' ' || c.фамилия AS customer_name,
       p.описание AS product_description,
       (p.стоимость * o.количество) AS total_amount,
       o."дата заказа" AS order_date
FROM "order" o
         JOIN customer c ON o.customer_id = c.id
         JOIN product p ON o.product_id = p.id
         JOIN order_status os ON o.статус = os.id
WHERE os."имя статуса" = 'В обработке'
  AND (p.стоимость * o.количество) > 10000
ORDER BY total_amount DESC;

-- === ЗАПРОСЫ НА ОБНОВЛЕНИЕ ===

-- 6. Обновление количества на складе при покупке (уменьшение количества товара на основе заказа).
UPDATE product
SET количество = количество - o.количество
    FROM "order" o
WHERE product.id = o.product_id
  AND o.id = 1;  -- Пример: обновить для конкретного заказа с ID=1

-- 7. Обновление статуса заказа на "Отправлен" для заказов старше 3 дней.
UPDATE "order"
SET статус = (SELECT id FROM order_status WHERE "имя статуса" = 'Отправлен')
WHERE "дата заказа" < CURRENT_DATE - INTERVAL '3 days'
  AND статус = (SELECT id FROM order_status WHERE "имя статуса" = 'В обработке');

-- 8. Обновление цены товара на 10% для товаров в категории "Электроника".
UPDATE product
SET стоимость = стоимость * 1.10
WHERE категория = 'Электроника';

-- === ЗАПРОСЫ НА УДАЛЕНИЕ ===

-- 9. Удаление клиентов без заказов (фильтрация с NOT EXISTS).
DELETE FROM customer c
WHERE NOT EXISTS (
    SELECT 1 FROM "order" o WHERE o.customer_id = c.id
);

-- 10. Удаление заказов со статусом "Отменен" старше 30 дней.
DELETE FROM "order" o
WHERE o.статус = (SELECT id FROM order_status WHERE "имя статуса" = 'Отменен')
  AND o."дата заказа" < CURRENT_DATE - INTERVAL '30 days';
