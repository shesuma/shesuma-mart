package com.shesuma.shesumamart.controller;

import com.shesuma.shesumamart.util.Database;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;

@WebServlet("/api/v1/health")
public class HealthServlet extends HttpServlet {

    @Override
    protected void doGet(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try (Connection connection =
                     Database.getDataSource().getConnection()) {

            if (connection.isValid(3)) {
                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(
                    "{\"status\":\"UP\",\"db\":\"UP\"}"
                );
            } else {
                response.setStatus(
                    HttpServletResponse.SC_SERVICE_UNAVAILABLE
                );
                response.getWriter().write(
                    "{\"status\":\"UP\",\"db\":\"DOWN\"}"
                );
            }

        } catch (Exception e) {
            response.setStatus(
                HttpServletResponse.SC_SERVICE_UNAVAILABLE
            );
            response.getWriter().write(
                "{\"status\":\"UP\",\"db\":\"DOWN\"}"
            );
        }
    }
}