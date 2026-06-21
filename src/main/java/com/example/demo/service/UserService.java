package com.example.demo.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.demo.helper.SecurityUtils;
import com.example.demo.model.User;
import com.example.demo.repo.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<User> getUsers() {
        return userRepository.findAll();
    }

    public void saveUser(User user) {

        if (user == null) {
            throw new IllegalArgumentException("User cannot be null");
        }

        // Validate required fields
        if (user.getUsername() == null || user.getUsername().trim().isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (user.getEmail() == null || user.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (user.getPassword() == null || user.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        // Check for existing username
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        // Check for existing email
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }

        // Hash password before saving
        String hashedPassword = SecurityUtils.hashPassword(user.getPassword());
        user.setPassword(hashedPassword);

        userRepository.save(user);
    }
    

    public User saveGoogleUser(User user) {

        if (user == null || user.getEmail() == null) {
            throw new IllegalArgumentException("Invalid Google user");
        }

        User existingUser = userRepository.findByEmail(user.getEmail());

        if (existingUser != null) {
            return existingUser;
        }

        User newUser = new User();
        newUser.setEmail(user.getEmail());

        newUser.setUsername(
            user.getUsername() != null ? user.getUsername() : user.getEmail().split("@")[0]
        );

        newUser.setPassword(null);
        newUser.setAuthProvider("GOOGLE");

        return userRepository.save(newUser);
    }

    public User findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public User loginUser(String email, String password) {

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException("Email cannot be null");
        }

        if (password == null || password.trim().isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        User existingUser = userRepository.findByEmail(email);

        if (existingUser == null) {
            throw new IllegalArgumentException("User does not exist");
        }

        // if(existingUser.getAuthProvider().equals("google")) {
        //     throw new IllegalArgumentException("User exists with google login");
        // }

        boolean passwordMatches = SecurityUtils.verifyPassword(
                password,
                existingUser.getPassword()
        );

        if (!passwordMatches) {
            throw new IllegalArgumentException("Invalid password");
        }

        return existingUser;
    }

}
