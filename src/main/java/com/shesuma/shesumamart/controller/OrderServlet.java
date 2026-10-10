

package com.shesuma.shesumamart.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shesuma.shesumamart.util.Database;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@WebServlet("/api/v1/orders")
public class OrderServlet extends HttpServlet {

    private final ObjectMapper mapper = new ObjectMapper();

    private void sendJson(HttpServletResponse response, int status,
                          Map<String, Object> data) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        mapper.writeValue(response.getWriter(), data);
    }

    @SuppressWarnings("unchecked")
    private Long getUserId(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return null;

        Object obj = session.getAttribute("user");
        if (!(obj instanceof Map)) return null;

        Object id = ((Map<String, Object>) obj).get("id");
        if (id == null) return null;

        try {
            return Long.valueOf(id.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    @Override
    protected void doGet(HttpServletRequest request,
                         HttpServletResponse response)
            throws ServletException, IOException {

        Long buyerId = getUserId(request);

        if (buyerId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Please login to view your orders.");
            sendJson(response, 401, result);
            return;
        }

        List<Map<String, Object>> orders = new ArrayList<>();

        String orderSql =
                "SELECT id, total_amount, status, created_at " +
                "FROM orders WHERE buyer_id = ? ORDER BY created_at DESC";

        String itemSql =
                "SELECT p.name, oi.quantity, oi.unit_price " +
                "FROM order_items oi JOIN products p ON p.id = oi.product_id " +
                "WHERE oi.order_id = ? ORDER BY oi.id";

        try (Connection con = Database.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(orderSql)) {

            ps.setLong(1, buyerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    long orderId = rs.getLong("id");

                    Map<String, Object> order = new HashMap<>();
                    order.put("id", orderId);
                    order.put("total", rs.getBigDecimal("total_amount"));
                    order.put("status", rs.getString("status"));

                    java.sql.Timestamp date = rs.getTimestamp("created_at");
                    order.put("date", date == null ? null : date.toString());

                    List<Map<String, Object>> items = new ArrayList<>();

                    try (PreparedStatement ips = con.prepareStatement(itemSql)) {
                        ips.setLong(1, orderId);

                        try (ResultSet irs = ips.executeQuery()) {
                            while (irs.next()) {
                                int qty = irs.getInt("quantity");
                                BigDecimal price = irs.getBigDecimal("unit_price");

                                Map<String, Object> item = new HashMap<>();
                                item.put("name", irs.getString("name"));
                                item.put("quantity", qty);
                                item.put("price", price);
                                item.put("subtotal",
                                        price.multiply(BigDecimal.valueOf(qty)));

                                items.add(item);
                            }
                        }
                    }

                    order.put("items", items);
                    orders.add(order);
                }
            }

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orders", orders);
            sendJson(response, 200, result);

        } catch (Exception e) {
            getServletContext().log("Unable to load orders", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Unable to load orders.");
            sendJson(response, 500, result);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response)
            throws ServletException, IOException {

        Long buyerId = getUserId(request);

        if (buyerId == null) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Please login before placing an order.");
            sendJson(response, 401, result);
            return;
        }

        List<Map<String, Object>> requestedItems;

        try {
            Map<String, Object> body = mapper.readValue(
                    request.getInputStream(),
                    new TypeReference<Map<String, Object>>() {});

            Object value = body.get("items");

            if (!(value instanceof List) || ((List<?>) value).isEmpty()) {
                throw new IllegalArgumentException("Your cart is empty.");
            }

            requestedItems = (List<Map<String, Object>>) value;

        } catch (Exception e) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Invalid order data. Check your cart.");
            sendJson(response, 400, result);
            return;
        }

        Connection con = null;

        try {
            con = Database.getDataSource().getConnection();
            con.setAutoCommit(false);

            BigDecimal subtotal = BigDecimal.ZERO;
            List<Map<String, Object>> validItems = new ArrayList<>();

            for (Map<String, Object> requested : requestedItems) {
                String name = requested.get("name") == null
                        ? "" : requested.get("name").toString().trim();

                int qty;
                try {
                    qty = Integer.parseInt(
                            String.valueOf(requested.get("quantity")));
                } catch (Exception e) {
                    throw new IllegalArgumentException("Invalid quantity.");
                }

                if (name.isEmpty() || qty < 1 || qty > 1000) {
                    throw new IllegalArgumentException("Invalid cart item.");
                }

                String sql =
                        "SELECT id, name, price, stock_qty FROM products " +
                        "WHERE LOWER(name) = LOWER(?) LIMIT 1 FOR UPDATE";

                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setString(1, name);

                    try (ResultSet rs = ps.executeQuery()) {
                        if (!rs.next()) {
                            throw new IllegalArgumentException(
                                    "Product not found: " + name);
                        }

                        if (rs.getInt("stock_qty") < qty) {
                            throw new IllegalArgumentException(
                                    "Not enough stock for " + name);
                        }

                        BigDecimal price = rs.getBigDecimal("price");
                        BigDecimal lineTotal =
                                price.multiply(BigDecimal.valueOf(qty));

                        Map<String, Object> item = new HashMap<>();
                        item.put("id", rs.getLong("id"));
                        item.put("name", rs.getString("name"));
                        item.put("quantity", qty);
                        item.put("price", price);

                        validItems.add(item);
                        subtotal = subtotal.add(lineTotal);
                    }
                }
            }

            BigDecimal delivery = subtotal.compareTo(
                    new BigDecimal("499")) >= 0
                    ? BigDecimal.ZERO : new BigDecimal("30");

            BigDecimal total = subtotal.add(delivery);
            long orderId;

            String insertOrder =
                    "INSERT INTO orders (buyer_id, total_amount, status) " +
                    "VALUES (?, ?, 'PENDING')";

            try (PreparedStatement ps = con.prepareStatement(
                    insertOrder, PreparedStatement.RETURN_GENERATED_KEYS)) {
                ps.setLong(1, buyerId);
                ps.setBigDecimal(2, total);
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) {
                        throw new SQLException("Order ID not generated.");
                    }
                    orderId = keys.getLong(1);
                }
            }

            String insertItem =
                    "INSERT INTO order_items " +
                    "(order_id, product_id, quantity, unit_price) " +
                    "VALUES (?, ?, ?, ?)";

            String updateStock =
                    "UPDATE products SET stock_qty = stock_qty - ? " +
                    "WHERE id = ? AND stock_qty >= ?";

            for (Map<String, Object> item : validItems) {
                long productId = ((Number) item.get("id")).longValue();
                int qty = (Integer) item.get("quantity");
                BigDecimal price = (BigDecimal) item.get("price");

                try (PreparedStatement ps = con.prepareStatement(insertItem)) {
                    ps.setLong(1, orderId);
                    ps.setLong(2, productId);
                    ps.setInt(3, qty);
                    ps.setBigDecimal(4, price);
                    ps.executeUpdate();
                }

                try (PreparedStatement ps = con.prepareStatement(updateStock)) {
                    ps.setInt(1, qty);
                    ps.setLong(2, productId);
                    ps.setInt(3, qty);

                    if (ps.executeUpdate() != 1) {
                        throw new IllegalArgumentException(
                                "Stock changed. Please try again.");
                    }
                }
            }

            con.commit();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("orderId", orderId);
            result.put("subtotal", subtotal);
            result.put("delivery", delivery);
            result.put("total", total);
            result.put("status", "PENDING");
            result.put("message", "Order placed successfully.");

            sendJson(response, 200, result);

        } catch (IllegalArgumentException e) {
            rollback(con);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", e.getMessage());
            sendJson(response, 400, result);

        } catch (Exception e) {
            rollback(con);
            getServletContext().log("Order placement failed", e);

            Map<String, Object> result = new HashMap<>();
            result.put("success", false);
            result.put("message", "Order failed. Please try again.");
            sendJson(response, 500, result);

        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ignored) {
                }
            }
        }
    }

    private void rollback(Connection con) {
        if (con != null) {
            try {
                con.rollback();
            } catch (SQLException ignored) {
            }
        }
    }
}
