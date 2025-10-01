package com.example;

import org.flywaydb.core.Flyway;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;

public class App {
    public static void main(String[] args) {
        Properties props = new Properties();
        try (InputStream input = App.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (input == null) {
                System.out.println("Извините, не удалось найти application.properties");
                return;
            }
            props.load(input);
        } catch (IOException ex) {
            ex.printStackTrace();
            return;
        }

        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String password = props.getProperty("db.password");
        boolean flywayEnabled = Boolean.parseBoolean(props.getProperty("flyway.enabled"));

        // Запускает промежуточные миграции, если они включены
        if (flywayEnabled) {
            Flyway flyway = Flyway.configure()
                    .dataSource(url, user, password)
                    .baselineOnMigrate(true)
                    .load();
            flyway.migrate();
            System.out.println("Миграция Flyway успешно завершена! 🚀");
        }

        Connection conn = null;
        try {
            // Подключиться к базе данных
            conn = DriverManager.getConnection(url, user, password);
            conn.setAutoCommit(false); // Начать транзакцию

            // 1. Ввод нового продукта и клиента
            System.out.println("1. Внедрение нового продукта и клиента...");
            String insertProductSQL = "INSERT INTO product (описание, стоимость, количество, категория) VALUES (?, ?, ?, ?) RETURNING id";
            PreparedStatement psProduct = conn.prepareStatement(insertProductSQL);
            psProduct.setString(1, "Смартфон");
            psProduct.setBigDecimal(2, new java.math.BigDecimal("30000.00"));
            psProduct.setInt(3, 5);
            psProduct.setString(4, "Электроника");
            ResultSet rsProduct = psProduct.executeQuery();
            int productId = -1;
            if (rsProduct.next()) {
                productId = rsProduct.getInt(1);
            }
            psProduct.close();

            String insertCustomerSQL = "INSERT INTO customer (имя, фамилия, телефон, email) VALUES (?, ?, ?, ?) RETURNING id";
            PreparedStatement psCustomer = conn.prepareStatement(insertCustomerSQL);
            psCustomer.setString(1, "Петр");
            psCustomer.setString(2, "Петров");
            psCustomer.setString(3, "+7-999-123-45-67");
            psCustomer.setString(4, "petr@example.com");
            ResultSet rsCustomer = psCustomer.executeQuery();
            int customerId = -1;
            if (rsCustomer.next()) {
                customerId = rsCustomer.getInt(1);
            }
            psCustomer.close();

            System.out.println("Вставленный продукт с ID: " + productId);
            System.out.println("Введенный клиент с ID: " + customerId);

            // Убедитесь, что статус заказа существует (укажите, если его нет).
            System.out.println("\nОбеспечение статуса заказа 'В обработке' существует...");
            String insertStatusSQL = "INSERT INTO order_status (\"имя статуса\") VALUES ('В обработке') ON CONFLICT (\"имя статуса\") DO NOTHING";
            PreparedStatement psStatus = conn.prepareStatement(insertStatusSQL);
            psStatus.executeUpdate();
            psStatus.close();
            System.out.println("Гарантированный статус заказа.");

            // 2. Создать заказ для клиента
            System.out.println("\n2. Создание заказа для клиента...");
            String insertOrderSQL = "INSERT INTO \"order\" (product_id, customer_id, \"дата заказа\", количество, статус) VALUES (?, ?, CURRENT_DATE, ?, (SELECT id FROM order_status WHERE \"имя статуса\" = 'В обработке')) RETURNING id";
            PreparedStatement psOrder = conn.prepareStatement(insertOrderSQL);
            psOrder.setInt(1, productId);
            psOrder.setInt(2, customerId);
            psOrder.setInt(3, 1);
            ResultSet rsOrder = psOrder.executeQuery();
            int orderId = -1;
            if (rsOrder.next()) {
                orderId = rsOrder.getInt(1);
            }
            psOrder.close();

            System.out.println("Созданный заказ с ID: " + orderId);

            // 3. Прочитайте и распечатайте последние 5 заказов с помощью JOIN
            System.out.println("\n3. Чтение последних 5 заказов с помощью JOIN...");
            String selectOrdersSQL = "SELECT o.id, c.имя, c.фамилия, p.описание, o.\"дата заказа\", o.количество, os.\"имя статуса\" FROM \"order\" o JOIN customer c ON o.customer_id = c.id JOIN product p ON o.product_id = p.id JOIN order_status os ON o.статус = os.id ORDER BY o.\"дата заказа\" DESC LIMIT 5";
            Statement stmt = conn.createStatement();
            ResultSet rsOrders = stmt.executeQuery(selectOrdersSQL);
            while (rsOrders.next()) {
                System.out.println("Order ID: " + rsOrders.getInt("id") +
                        ", Customer: " + rsOrders.getString("имя") + " " + rsOrders.getString("фамилия") +
                        ", Product: " + rsOrders.getString("описание") +
                        ", Date: " + rsOrders.getDate("дата заказа") +
                        ", Quantity: " + rsOrders.getInt("количество") +
                        ", Status: " + rsOrders.getString("имя статуса"));
            }
            rsOrders.close();
            stmt.close();

            // 4. Обновите цену и количество товара
            System.out.println("\n4. Обновление цены и количества товара...");
            String updateProductSQL = "UPDATE product SET стоимость = стоимость * 1.1, количество = количество - 1 WHERE id = ?";
            PreparedStatement psUpdate = conn.prepareStatement(updateProductSQL);
            psUpdate.setInt(1, productId);
            int updatedRows = psUpdate.executeUpdate();
            psUpdate.close();

            System.out.println("Обновленный " + updatedRows + " продукт(ы).");

            // 5. Удалить тестовые записи (те, которые мы вставили)
            System.out.println("\n5. Удаление тестовых записей...");
            String deleteOrderSQL = "DELETE FROM \"order\" WHERE id = ?";
            PreparedStatement psDeleteOrder = conn.prepareStatement(deleteOrderSQL);
            psDeleteOrder.setInt(1, orderId);
            psDeleteOrder.executeUpdate();
            psDeleteOrder.close();

            String deleteProductSQL = "DELETE FROM product WHERE id = ?";
            PreparedStatement psDeleteProduct = conn.prepareStatement(deleteProductSQL);
            psDeleteProduct.setInt(1, productId);
            psDeleteProduct.executeUpdate();
            psDeleteProduct.close();

            String deleteCustomerSQL = "DELETE FROM customer WHERE id = ?";
            PreparedStatement psDeleteCustomer = conn.prepareStatement(deleteCustomerSQL);
            psDeleteCustomer.setInt(1, customerId);
            psDeleteCustomer.executeUpdate();
            psDeleteCustomer.close();

            System.out.println("Удаленные тестовые записи.");

            // Фиксация транзакции
            conn.commit();
            System.out.println("\nТранзакция успешно совершена! ✅");

        } catch (SQLException e) {
            System.out.println("Произошла ошибка: " + e.getMessage());
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("Откат транзакции. ❌");
                } catch (SQLException ex) {
                    System.out.println("Ошибка при откате: " + ex.getMessage());
                }
            }
        } finally {
            if (conn != null) {
                try {
                    conn.close();
                } catch (SQLException e) {
                    System.out.println("Ошибка при закрытии соединения: " + e.getMessage());
                }
            }
        }
    }
}
