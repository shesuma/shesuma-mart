
package com.shesuma.shesumamart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shesuma.shesumamart.dao.JdbcUserDao;
import com.shesuma.shesumamart.dao.UserDao;
import com.shesuma.shesumamart.model.User;
import com.shesuma.shesumamart.service.AuthService;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@WebServlet("/api/v1/auth/register")
public class RegisterServlet extends HttpServlet {

    private final UserDao userDao = new JdbcUserDao();
    private final AuthService authService = new AuthService();
    private final ObjectMapper mapper = new ObjectMapper();

    @Override
    protected void doPost(HttpServletRequest request,
                          HttpServletResponse response) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> result = new HashMap<>();

        try {
            Map<?, ?> body = mapper.readValue(
                request.getInputStream(), Map.class
            );

            String name = value(body, "name");
            String email = value(body, "email").toLowerCase(Locale.ROOT);
            String password = value(body, "password");
            String role = value(body, "role").toUpperCase(Locale.ROOT);

            if (name.isBlank() || email.isBlank()
                    || password.isBlank()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("success", false);
                result.put("message", "Name, email and password are required.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("success", false);
                result.put("message", "Enter a valid email address.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            if (password.length() < 8 || password.length() > 72) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("success", false);
                result.put("message", "Password must be 8 to 72 characters.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            if (!role.equals("BUYER") && !role.equals("SELLER")) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("success", false);
                result.put("message", "Choose Buyer or Seller.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            Optional<User> existing = userDao.findByEmail(email);
            if (existing.isPresent()) {
                response.setStatus(HttpServletResponse.SC_CONFLICT);
                result.put("success", false);
                result.put("message", "This email is already registered.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPasswordHash(authService.hashPassword(password));
            user.setRole(role);

            long id = userDao.save(user);

            result.put("success", true);
            result.put("message", "Registration successful. Please log in.");
            result.put("userId", id);

            response.setStatus(HttpServletResponse.SC_CREATED);
            mapper.writeValue(response.getOutputStream(), result);

        } catch (SQLException e) {
            getServletContext().log("Registration database error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("message", "Registration failed. Please try again.");
            mapper.writeValue(response.getOutputStream(), result);

        } catch (Exception e) {
            getServletContext().log("Registration request error", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("message", "Invalid registration request.");
            mapper.writeValue(response.getOutputStream(), result);
        }
    }

    private String value(Map<?, ?> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }
}