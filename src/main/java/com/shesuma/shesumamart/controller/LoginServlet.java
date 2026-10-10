
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
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@WebServlet("/api/v1/auth/login")
public class LoginServlet extends HttpServlet {

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

            String email = value(body, "email").toLowerCase(Locale.ROOT);
            String password = value(body, "password");

            if (email.isBlank() || password.isBlank()) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                result.put("success", false);
                result.put("message", "Email and password are required.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            Optional<User> found = userDao.findByEmail(email);

            if (found.isEmpty()
                    || !authService.verifyPassword(
                        password, found.get().getPasswordHash())) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                result.put("success", false);
                result.put("message", "Incorrect email or password.");
                mapper.writeValue(response.getOutputStream(), result);
                return;
            }

            User user = found.get();

            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }

            HttpSession session = request.getSession(true);
            request.changeSessionId();
            session.setMaxInactiveInterval(30 * 60);

            Map<String, Object> safeUser = new HashMap<>();
            safeUser.put("id", user.getId());
            safeUser.put("name", user.getName());
            safeUser.put("email", user.getEmail());
            safeUser.put("role", user.getRole());

            session.setAttribute("user", safeUser);

            result.put("success", true);
            result.put("message", "Login successful.");
            result.put("user", safeUser);

            mapper.writeValue(response.getOutputStream(), result);

        } catch (SQLException e) {
            getServletContext().log("Login database error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            result.put("success", false);
            result.put("message", "Login failed. Please try again.");
            mapper.writeValue(response.getOutputStream(), result);

        } catch (Exception e) {
            getServletContext().log("Login request error", e);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            result.put("success", false);
            result.put("message", "Invalid login request.");
            mapper.writeValue(response.getOutputStream(), result);
        }
    }

    private String value(Map<?, ?> body, String key) {
        Object value = body.get(key);
        return value == null ? "" : value.toString().trim();
    }
}
