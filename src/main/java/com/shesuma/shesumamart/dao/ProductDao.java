package com.shesuma.shesumamart.dao;

import com.shesuma.shesumamart.model.Product;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

public interface ProductDao {
    List<Product> findAll() throws SQLException;
    Optional<Product> findById(long id) throws SQLException;
}
