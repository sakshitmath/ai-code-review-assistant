package com.aicodereview.backend.service;

import com.aicodereview.backend.dto.ResetPasswordRequest;
import com.aicodereview.backend.dto.UpdateProfileRequest;
import com.aicodereview.backend.entity.User;
import com.aicodereview.backend.exception.ApiException;
import com.aicodereview.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User getByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException("User not found"));
    }

    public Map<String, Object> updateProfile(String email, UpdateProfileRequest request) {
        User user = getByEmail(email);
        user.setName(request.getName());
        User saved = userRepository.save(user);

        return Map.of(
                "id", saved.getId(),
                "name", saved.getName(),
                "email", saved.getEmail(),
                "message", "Profile updated successfully"
        );
    }

    public Map<String, String> resetPassword(String email, ResetPasswordRequest request) {
        User user = getByEmail(email);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new ApiException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        return Map.of("message", "Password reset successfully");
    }
}