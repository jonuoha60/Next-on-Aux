package com.example.demo.helper;

import java.util.Arrays;
import java.util.Random;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

public class SecurityUtils {
   
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder(12);

    public SecurityUtils() {
    }

   public static String getCookieValue(HttpServletRequest request, String cookieName) {

    Cookie[] cookies = request.getCookies();

    if (cookies == null || cookieName == null) {
        return null;
    }

    return Arrays.stream(cookies)
            .filter(c -> cookieName.equals(c.getName()))
            .map(Cookie::getValue)
            .findFirst()
            .orElse(null);
}
    
    public static String hashPassword(String plaintextPassword) {
        return encoder.encode(plaintextPassword);
    }

    public static boolean verifyPassword(String plaintextPassword, String hashedPassword) {
        return encoder.matches(plaintextPassword, hashedPassword);
    }

    private static String s4() {
        int value = (int) ((1 + Math.random() * 0x10000));
        return Integer.toHexString(value).substring(1);
    }

    public static String secureCode() {
        return s4() + s4() + "-" +
               s4() + "-" +
               s4() + "-" +
               s4() + "-" +
               s4() + s4() + s4();
    }

     public static String generateUID() {
        Random rand = new Random();
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder result = new StringBuilder();

        for (int i = 0; i < 8; i++) {
            result.append(chars.charAt(rand.nextInt(chars.length())));
        }
        
    
        return result.toString();
    }


}
