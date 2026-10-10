package com.shesuma.shesumamart.service;

import org.mindrot.jbcrypt.BCrypt;

public class AuthService {
    public String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt(12));
    }

    public boolean verifyPassword(String password, String hash) {
        return password != null && hash != null && BCrypt.checkpw(password, hash);
    }
}
