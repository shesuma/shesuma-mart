package com.shesuma.shesumamart.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shesuma.shesumamart.util.Database;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;
import java.io.IOException;
import java.sql.*;
import java.util.*;

@WebServlet("/api/v1/products")
public class ProductServlet extends HttpServlet {
    private final ObjectMapper mapper = new ObjectMapper();
    private static final String[][] CATALOG = {
        {"Tomato","Vegetables","30","kg","https://images.unsplash.com/photo-1546094096-0df4bcaaa337?auto=format&fit=crop&w=600&q=85","50"},
        {"Potato","Vegetables","35","kg","https://images.unsplash.com/photo-1518977676601-b53f82aba655?auto=format&fit=crop&w=600&q=85","50"},
        {"Onion","Vegetables","40","kg","https://images.unsplash.com/photo-1508747703725-719777637510?auto=format&fit=crop&w=600&q=85","50"},
        {"Carrot","Vegetables","50","kg","https://images.unsplash.com/photo-1445282768818-728615cc910a?auto=format&fit=crop&w=600&q=85","40"},
        {"Cucumber","Vegetables","35","kg","https://images.unsplash.com/photo-1604977042946-1eecc30f269e?auto=format&fit=crop&w=600&q=85","40"},
        {"Apple","Fruits","180","kg","https://images.unsplash.com/photo-1560806887-1e4cd0b6cbd6?auto=format&fit=crop&w=600&q=85","30"},
        {"Banana","Fruits","60","dozen","https://images.unsplash.com/photo-1571771894821-ce9b6c11b08e?auto=format&fit=crop&w=600&q=85","30"},
        {"Orange","Fruits","100","kg","https://images.unsplash.com/photo-1547514701-42782101795e?auto=format&fit=crop&w=600&q=85","30"},
        {"Grapes","Fruits","110","kg","https://images.unsplash.com/photo-1537640538966-79f369143f8f?auto=format&fit=crop&w=600&q=85","25"},
        {"Mango","Fruits","120","kg","https://images.unsplash.com/photo-1553279768-865429fa0078?auto=format&fit=crop&w=600&q=85","25"},
        {"Basmati Rice","Rice & Staples","120","kg","https://images.unsplash.com/photo-1586201375761-83865001e31c?auto=format&fit=crop&w=600&q=85","40"},
        {"Wheat Flour","Rice & Staples","48","kg","https://images.unsplash.com/photo-1627485937980-221c88ac04f9?auto=format&fit=crop&w=600&q=85","40"},
        {"Toor Dal","Rice & Staples","140","kg","https://images.unsplash.com/photo-1515543904379-3d757afe72e4?auto=format&fit=crop&w=600&q=85","25"},
        {"Milk","Dairy","30","500 ml","https://images.unsplash.com/photo-1563636619-e9143da7973b?auto=format&fit=crop&w=600&q=85","40"},
        {"Butter","Dairy","55","100 g","https://images.unsplash.com/photo-1589985270826-4b7bb135bc9d?auto=format&fit=crop&w=600&q=85","20"},
        {"Eggs","Dairy","85","6 pieces","https://images.unsplash.com/photo-1506976785307-8732e854ad03?auto=format&fit=crop&w=600&q=85","25"},
        {"Bread","Snacks","40","loaf","https://images.unsplash.com/photo-1509440159596-0249088772ff?auto=format&fit=crop&w=600&q=85","25"},
        {"Almonds","Snacks","210","250 g","https://images.unsplash.com/photo-1508061253366-f7da158b6d46?auto=format&fit=crop&w=600&q=85","15"},
        {"Cooking Oil","Cooking Essentials","145","1 litre","https://images.unsplash.com/photo-1474979266404-7eaacbcd87c5?auto=format&fit=crop&w=600&q=85","20"},
        {"Turmeric Powder","Cooking Essentials","45","100 g","https://images.unsplash.com/photo-1615485500704-8e990f9900f2?auto=format&fit=crop&w=600&q=85","20"}
    };

    @Override protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        try (Connection c = Database.getDataSource().getConnection()) {
            seedCatalog(c);
            List<Map<String,Object>> list = new ArrayList<>();
            try (PreparedStatement p = c.prepareStatement("SELECT id,name,description,price,stock_qty,category,image_url FROM products ORDER BY id"); ResultSet r = p.executeQuery()) {
                while (r.next()) {
                    Map<String,Object> m = new LinkedHashMap<>();
                    m.put("id", r.getLong("id")); m.put("name", r.getString("name"));
                    m.put("unit", r.getString("description") == null || r.getString("description").isBlank() ? "item" : r.getString("description"));
                    m.put("price", r.getBigDecimal("price")); m.put("stock", r.getInt("stock_qty"));
                    m.put("category", r.getString("category")); m.put("image", r.getString("image_url"));
                    m.put("rating", 4.5); list.add(m);
                }
            }
            mapper.writeValue(resp.getOutputStream(), Map.of("success",true,"products",list));
        } catch (Exception e) { getServletContext().log("Products GET failed",e); resp.setStatus(500); mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Unable to load products.")); }
    }

    @Override protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8");
        Map<?,?> body;
        try { body = mapper.readValue(req.getInputStream(), Map.class); } catch (Exception e) { resp.setStatus(400); mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Invalid product details.")); return; }
        Map<?,?> user = sessionUser(req);
        if (user == null || !"SELLER".equalsIgnoreCase(String.valueOf(user.get("role")))) { resp.setStatus(401); mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Please log in with a seller account.")); return; }
        String name = str(body,"name"), category = str(body,"category"), unit = str(body,"unit");
        String image = str(body,"image"); double price; int stock;
        try { price=Double.parseDouble(String.valueOf(body.get("price"))); stock=Integer.parseInt(String.valueOf(body.get("stock"))); } catch(Exception e) { resp.setStatus(400); mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Price and stock must be valid numbers.")); return; }
        if(name.isBlank()||category.isBlank()||unit.isBlank()||price<=0||stock<0||name.length()>180||category.length()>80||unit.length()>100) { resp.setStatus(400); mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Check product name, category, price, stock and unit.")); return; }
        try(Connection c=Database.getDataSource().getConnection(); PreparedStatement p=c.prepareStatement("INSERT INTO products(seller_id,name,description,price,stock_qty,category,image_url) VALUES(?,?,?,?,?,?,?)",Statement.RETURN_GENERATED_KEYS)) {
            p.setLong(1,Long.parseLong(String.valueOf(user.get("id")))); p.setString(2,name); p.setString(3,unit); p.setBigDecimal(4,java.math.BigDecimal.valueOf(price)); p.setInt(5,stock); p.setString(6,category); p.setString(7,image.isBlank()?"https://placehold.co/600x400/f1f5f1/176b45?text=Grocery":image); p.executeUpdate();
            try(ResultSet keys=p.getGeneratedKeys()){ long id=keys.next()?keys.getLong(1):0; resp.setStatus(201); mapper.writeValue(resp.getOutputStream(),Map.of("success",true,"message","Product saved to database.","id",id)); }
        } catch(Exception e) { getServletContext().log("Product POST failed",e); resp.setStatus(500); mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Could not save product to database.")); }
    }

    @Override protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("application/json;charset=UTF-8"); Map<?,?> user=sessionUser(req);
        if(user==null||!"SELLER".equalsIgnoreCase(String.valueOf(user.get("role")))){resp.setStatus(401);mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Seller login required."));return;}
        long id; try{id=Long.parseLong(req.getParameter("id"));}catch(Exception e){resp.setStatus(400);mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Invalid product ID."));return;}
        try(Connection c=Database.getDataSource().getConnection();PreparedStatement p=c.prepareStatement("DELETE FROM products WHERE id=? AND seller_id=?")){p.setLong(1,id);p.setLong(2,Long.parseLong(String.valueOf(user.get("id"))));int n=p.executeUpdate();if(n==0){resp.setStatus(404);mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Product not found or not owned by this seller."));}else mapper.writeValue(resp.getOutputStream(),Map.of("success",true,"message","Product deleted."));}
        catch(Exception e){getServletContext().log("Product DELETE failed",e);resp.setStatus(500);mapper.writeValue(resp.getOutputStream(),Map.of("success",false,"message","Could not delete product."));}
    }
    private void seedCatalog(Connection c) throws SQLException {
        long sellerId=-1; try(PreparedStatement p=c.prepareStatement("SELECT MIN(id) FROM users WHERE UPPER(role)='SELLER'");ResultSet r=p.executeQuery()){if(r.next())sellerId=r.getLong(1);if(r.wasNull())sellerId=-1;}
        // Keep the catalogue available for existing installations that have users but no seller account yet.
        if(sellerId<0) try(PreparedStatement p=c.prepareStatement("SELECT MIN(id) FROM users");ResultSet r=p.executeQuery()){if(r.next())sellerId=r.getLong(1);if(r.wasNull())sellerId=-1;}
        if(sellerId<0)return;
        String exists="SELECT 1 FROM products WHERE LOWER(name)=LOWER(?)";
        String insert="INSERT INTO products(seller_id,name,description,price,stock_qty,category,image_url) VALUES(?,?,?,?,?,?,?)";
        for(String[] x:CATALOG){boolean found=false;try(PreparedStatement p=c.prepareStatement(exists)){p.setString(1,x[0]);try(ResultSet r=p.executeQuery()){found=r.next();}}if(!found)try(PreparedStatement p=c.prepareStatement(insert)){p.setLong(1,sellerId);p.setString(2,x[0]);p.setString(3,x[3]);p.setBigDecimal(4,new java.math.BigDecimal(x[2]));p.setInt(5,Integer.parseInt(x[5]));p.setString(6,x[1]);p.setString(7,x[4]);p.executeUpdate();}}
    }
    @SuppressWarnings("unchecked") private Map<?,?> sessionUser(HttpServletRequest req){HttpSession s=req.getSession(false);Object u=s==null?null:s.getAttribute("user");return u instanceof Map?(Map<?,?>)u:null;}
    private String str(Map<?,?> m,String k){Object v=m.get(k);return v==null?"":String.valueOf(v).trim();}
}
