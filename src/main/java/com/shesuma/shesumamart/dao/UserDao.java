package com.shesuma.shesumamart.dao;

import com.shesuma.shesumamart.model.User;
import java.sql.SQLException;
import java.util.Optional;

public interface UserDao {
    Optional<User> findByEmail(String email) throws SQLException;
    long save(User user) throws SQLException;
}
