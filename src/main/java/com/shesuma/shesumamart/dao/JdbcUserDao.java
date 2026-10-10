
package com.shesuma.shesumamart.dao;

import com.shesuma.shesumamart.model.User;
import com.shesuma.shesumamart.util.Database;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Optional;

public class JdbcUserDao implements UserDao {

    @Override
    public Optional<User> findByEmail(String email) throws SQLException {
        String sql = """
            SELECT id, name, email, password_hash, role
            FROM users
            WHERE LOWER(email) = LOWER(?)
            """;

        try (Connection con = Database.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {

            ps.setString(1, email.trim());

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    User user = new User(
                        rs.getLong("id"),
                        rs.getString("name"),
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role")
                    );
                    return Optional.of(user);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public long save(User user) throws SQLException {
        String sql = """
            INSERT INTO users (name, email, password_hash, role)
            VALUES (?, ?, ?, ?)
            """;

        try (Connection con = Database.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(
                 sql, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, user.getName().trim());
            ps.setString(2, user.getEmail().trim().toLowerCase());
            ps.setString(3, user.getPasswordHash());
            ps.setString(4, user.getRole());

            int rows = ps.executeUpdate();

            if (rows != 1) {
                throw new SQLException("User registration failed.");
            }

            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    long id = keys.getLong(1);
                    user.setId(id);
                    return id;
                }
            }

            throw new SQLException("Could not retrieve the new user ID.");
        }
    }
}

