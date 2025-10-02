package com.example;

import org.flywaydb.core.Flyway;

import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDate;
import java.util.Properties;

public class App {
    private static String DB_URL;
    private static String DB_USER;
    private static String DB_PASSWORD;

    public static void main(String[] args) {
        loadProperties();

        // Запускаем миграции Flyway
        Flyway flyway = Flyway.configure()
                .dataSource(DB_URL, DB_USER, DB_PASSWORD)
                .load();
        flyway.migrate();

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            conn.setAutoCommit(false);  // Включаем транзакцию

            try {
                // 1. Вставка нового товара и покупателя
                int productId = insertProduct(conn, "Смартфон", 30000.00, 20, "Электроника");
                int customerId = insertCustomer(conn, "Алексей", "Петров", "alexey.petrov@example.com", "1234567890");

                // 2. Создание заказа для покупателя
                int orderId = createOrder(conn, productId, customerId, LocalDate.now(), 2, 1); // статус = 1 ("В обработке")

                // 3. Чтение и вывод последних 5 заказов с JOIN на товары и покупателей
                printLastFiveOrders(conn);

                // 4. Обновление цены товара и количества на складе
                updateProductPriceAndQuantity(conn, productId, 28000.00, 18);

                // Дополнительно: 5 запросов на чтение
                readQueries(conn);

                // 3 запроса на обновление (включая выше)
                updateQueries(conn, productId);

                // 2 запроса на удаление
                deleteQueries(conn, customerId, productId);

                conn.commit();
            } catch (Exception e) {
                System.err.println("Ошибка во время транзакции: " + e.getMessage());
                e.printStackTrace();
                conn.rollback();
                System.out.println("Транзакция отменена.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void loadProperties() {
        Properties props = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                throw new RuntimeException("Не найден файл application.properties в classpath");
            }
            props.load(input);
            DB_URL = props.getProperty("db.url");
            DB_USER = props.getProperty("db.username");
            DB_PASSWORD = props.getProperty("db.password");
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при загрузке application.properties", e);
        }
    }

    // Вставка товара, возвращает ID
    private static int insertProduct(Connection conn, String description, double price, int quantity, String category) throws SQLException {
        String sql = "INSERT INTO product (описание, стоимость, количество, категория) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, description);
            ps.setDouble(2, price);
            ps.setInt(3, quantity);
            ps.setString(4, category);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            System.out.printf("Вставлен товар: ID=%d, описание='%s', цена=%.2f, количество=%d, категория='%s'%n",
                    id, description, price, quantity, category);
            return id;
        }
    }

    // Вставка покупателя, возвращает ID
    private static int insertCustomer(Connection conn, String firstName, String lastName, String email, String phone) throws SQLException {
        String sql = "INSERT INTO customer (имя, фамилия, email, телефон) VALUES (?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, firstName);
            ps.setString(2, lastName);
            ps.setString(3, email);
            ps.setString(4, phone);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            System.out.printf("Вставлен покупатель: ID=%d, %s %s, email='%s', телефон='%s'%n",
                    id, firstName, lastName, email, phone);
            return id;
        }
    }

    // Создание заказа, возвращает ID
    private static int createOrder(Connection conn, int productId, int customerId, LocalDate orderDate, int quantity, int statusId) throws SQLException {
        String sql = "INSERT INTO \"order\" (product_id, customer_id, \"дата заказа\", количество, статус) VALUES (?, ?, ?, ?, ?) RETURNING id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, productId);
            ps.setInt(2, customerId);
            ps.setDate(3, Date.valueOf(orderDate));
            ps.setInt(4, quantity);
            ps.setInt(5, statusId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            int id = rs.getInt(1);
            System.out.printf("Создан заказ: ID=%d, product_id=%d, customer_id=%d, дата=%s, количество=%d, статус=%d%n",
                    id, productId, customerId, orderDate, quantity, statusId);
            return id;
        }
    }

    // Вывод последних 5 заказов с JOIN на товары и покупателей
    private static void printLastFiveOrders(Connection conn) throws SQLException {
        String sql = """
                SELECT o.id, c.имя, c.фамилия, p.описание, o."дата заказа", o.количество, os."имя статуса"
                FROM "order" o
                JOIN customer c ON o.customer_id = c.id
                JOIN product p ON o.product_id = p.id
                JOIN order_status os ON o.статус = os.id
                ORDER BY o."дата заказа" DESC
                LIMIT 5
                """;
        System.out.println("\nПоследние 5 заказов:");
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("Заказ ID=%d: Покупатель: %s %s, Товар: %s, Дата: %s, Количество: %d, Статус: %s%n",
                        rs.getInt("id"),
                        rs.getString("имя"),
                        rs.getString("фамилия"),
                        rs.getString("описание"),
                        rs.getDate("дата заказа"),
                        rs.getInt("количество"),
                        rs.getString("имя статуса"));
            }
        }
    }

    // Обновление цены и количества товара
    private static void updateProductPriceAndQuantity(Connection conn, int productId, double newPrice, int newQuantity) throws SQLException {
        String sql = "UPDATE product SET стоимость = ?, количество = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setInt(2, newQuantity);
            ps.setInt(3, productId);
            int updated = ps.executeUpdate();
            System.out.printf("Обновлено товара ID=%d: новая цена=%.2f, новое количество=%d (обновлено строк: %d)%n",
                    productId, newPrice, newQuantity, updated);
        }
    }

    // Дополнительные 5 запросов на чтение
    private static void readQueries(Connection conn) throws SQLException {
        System.out.println("\nДополнительные запросы на чтение:");

        // 1. Список всех заказов за последние 7 дней с именем покупателя и описанием товара
        String sql1 = """
                SELECT o.id, c.имя, c.фамилия, p.описание, o."дата заказа"
                FROM "order" o
                JOIN customer c ON o.customer_id = c.id
                JOIN product p ON o.product_id = p.id
                WHERE o."дата заказа" >= CURRENT_DATE - INTERVAL '7 days'
                ORDER BY o."дата заказа" DESC
                """;
        System.out.println("Заказы за последние 7 дней:");
        try (PreparedStatement ps = conn.prepareStatement(sql1);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("Заказ ID=%d, Покупатель: %s %s, Товар: %s, Дата: %s%n",
                        rs.getInt("id"),
                        rs.getString("имя"),
                        rs.getString("фамилия"),
                        rs.getString("описание"),
                        rs.getDate("дата заказа"));
            }
        }

        // 2. Топ-3 самых популярных товаров (по суммарному количеству заказов)
        String sql2 = """
                SELECT p.описание, SUM(o.количество) AS total_ordered
                FROM "order" o
                JOIN product p ON o.product_id = p.id
                GROUP BY p.описание
                ORDER BY total_ordered DESC
                LIMIT 3
                """;
        System.out.println("\nТоп-3 самых популярных товара:");
        try (PreparedStatement ps = conn.prepareStatement(sql2);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("Товар: %s, Заказано всего: %d%n",
                        rs.getString("описание"),
                        rs.getInt("total_ordered"));
            }
        }

        // 3. Количество клиентов без заказов
        String sql3 = """
                SELECT COUNT(*) AS no_order_customers
                FROM customer c
                LEFT JOIN "order" o ON c.id = o.customer_id
                WHERE o.id IS NULL
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql3);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            System.out.printf("\nКлиентов без заказов: %d%n", rs.getInt("no_order_customers"));
        }

        // 4. Средняя стоимость товаров по категориям
        String sql4 = """
                SELECT категория, AVG(стоимость) AS avg_price
                FROM product
                GROUP BY категория
                """;
        System.out.println("\nСредняя стоимость товаров по категориям:");
        try (PreparedStatement ps = conn.prepareStatement(sql4);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("Категория: %s, Средняя цена: %.2f%n",
                        rs.getString("категория"),
                        rs.getDouble("avg_price"));
            }
        }

        // 5. Заказы с фильтрацией по статусу "В обработке"
        String sql5 = """
                SELECT o.id, c.имя, c.фамилия, p.описание, o."дата заказа"
                FROM "order" o
                JOIN customer c ON o.customer_id = c.id
                JOIN product p ON o.product_id = p.id
                JOIN order_status os ON o.статус = os.id
                WHERE os."имя статуса" = 'В обработке'
                ORDER BY o."дата заказа" DESC
                """;
        System.out.println("\nЗаказы со статусом 'В обработке':");
        try (PreparedStatement ps = conn.prepareStatement(sql5);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.printf("Заказ ID=%d, Покупатель: %s %s, Товар: %s, Дата: %s%n",
                        rs.getInt("id"),
                        rs.getString("имя"),
                        rs.getString("фамилия"),
                        rs.getString("описание"),
                        rs.getDate("дата заказа"));
            }
        }
    }

    // 3 запроса на обновление (обновление статуса заказа, обновление телефона клиента, обновление цены товара)
    private static void updateQueries(Connection conn, int productId) throws SQLException {
        System.out.println("\nЗапросы на обновление:");

        // 1. Обновление статуса заказа (например, последний заказ в статус "Отправлен" (id=2))
        String sql1 = "UPDATE \"order\" SET статус = 2 WHERE id = (SELECT MAX(id) FROM \"order\")";
        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            int updated = ps.executeUpdate();
            System.out.printf("Обновлен статус последнего заказа на 'Отправлен' (обновлено строк: %d)%n", updated);
        }

        // 2. Обновление телефона клиента
        String sql2 = "UPDATE customer SET телефон = ? WHERE id = (SELECT MAX(id) FROM customer)";
        try (PreparedStatement ps = conn.prepareStatement(sql2)) {
            ps.setString(1, "+7-999-888-7777");
            int updated = ps.executeUpdate();
            System.out.printf("Обновлен телефон последнего клиента (обновлено строк: %d)%n", updated);
        }

        // 3. Обновление цены товара (уже выполнено выше, но повторим для демонстрации)
        updateProductPriceAndQuantity(conn, productId, 27000.00, 15);
    }

    // 2 запроса на удаление (удаление клиентов без заказов, удаление тестовых записей)
    private static void deleteQueries(Connection conn, int customerId, int productId) throws SQLException {
        System.out.println("\nЗапросы на удаление:");

        // 1. Удаление клиентов без заказов
        String sql1 = """
                DELETE FROM customer c
                WHERE NOT EXISTS (
                    SELECT 1 FROM "order" o WHERE o.customer_id = c.id
                )
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql1)) {
            int deleted = ps.executeUpdate();
            System.out.printf("Удалено клиентов без заказов: %d%n", deleted);
        }

        // 2. Удаление тестовых записей: заказ, клиент, товар (созданные в этом запуске)
        // Удаляем заказ
        String sqlDeleteOrder = "DELETE FROM \"order\" WHERE customer_id = ? AND product_id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDeleteOrder)) {
            ps.setInt(1, customerId);
            ps.setInt(2, productId);
            int deleted = ps.executeUpdate();
            System.out.printf("Удалено заказов тестовых: %d%n", deleted);
        }

        // Удаляем клиента
        String sqlDeleteCustomer = "DELETE FROM customer WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDeleteCustomer)) {
            ps.setInt(1, customerId);
            int deleted = ps.executeUpdate();
            System.out.printf("Удалено клиентов тестовых: %d%n", deleted);
        }

        // Удаляем товар
        String sqlDeleteProduct = "DELETE FROM product WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlDeleteProduct)) {
            ps.setInt(1, productId);
            int deleted = ps.executeUpdate();
            System.out.printf("Удалено товаров тестовых: %d%n", deleted);
        }
    }
}

