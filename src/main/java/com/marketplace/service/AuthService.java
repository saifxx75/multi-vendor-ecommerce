package com.marketplace.service;

import com.marketplace.dto.AuthResponse;
import com.marketplace.dto.LoginRequest;
import com.marketplace.dto.RegisterRequest;
import com.marketplace.entity.Role;
import com.marketplace.entity.User;
import com.marketplace.entity.Vendor;
import com.marketplace.entity.VendorStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ConflictException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.UserRepository;
import com.marketplace.repository.VendorRepository;
import com.marketplace.security.JwtService;
import com.marketplace.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final VendorRepository vendorRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    /**
     * Public sign-up for customers and vendors. Admins can never be created here.
     * A new vendor starts as PENDING and cannot sell until an admin approves the store.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.role() == Role.ADMIN) {
            throw new BadRequestException("Admin accounts cannot be self-registered");
        }

        String email = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(email)) {
            throw new ConflictException("Email is already registered");
        }

        String storeName = request.storeName() == null ? null : request.storeName().trim();
        if (request.role() == Role.VENDOR) {
            if (storeName == null || storeName.isEmpty()) {
                throw new BadRequestException("storeName is required for vendor registration");
            }
            if (vendorRepository.existsByStoreNameIgnoreCase(storeName)) {
                throw new ConflictException("Store name is already taken");
            }
        }

        User user = userRepository.save(new User(
                request.name().trim(), email, passwordEncoder.encode(request.password()), request.role()));

        if (request.role() == Role.VENDOR) {
            Vendor vendor = new Vendor();
            vendor.setUser(user);
            vendor.setStoreName(storeName);
            vendor.setDescription(request.storeDescription());
            vendor.setStatus(VendorStatus.PENDING);
            vendorRepository.save(vendor);
        }

        return toAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Throws AuthenticationException (-> 401) on unknown email, wrong password or disabled account.
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        return toAuthResponse(user);
    }

    private AuthResponse toAuthResponse(User user) {
        String token = jwtService.generateToken(UserPrincipal.from(user));
        return new AuthResponse(token, "Bearer", user.getId(), user.getName(), user.getEmail(), user.getRole());
    }
}
