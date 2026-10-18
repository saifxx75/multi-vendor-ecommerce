package com.marketplace.service;

import com.marketplace.dto.AuthResponse;
import com.marketplace.dto.RegisterRequest;
import com.marketplace.entity.Role;
import com.marketplace.entity.User;
import com.marketplace.entity.Vendor;
import com.marketplace.entity.VendorStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ConflictException;
import com.marketplace.repository.UserRepository;
import com.marketplace.repository.VendorRepository;
import com.marketplace.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private VendorRepository vendorRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("admin accounts cannot be self-registered")
    void registerAdminIsRejected() {
        RegisterRequest request = new RegisterRequest("Eve", "eve@example.com", "password123", Role.ADMIN, null, null);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(BadRequestException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("registering with an existing email is a conflict")
    void registerDuplicateEmailIsConflict() {
        when(userRepository.existsByEmail("bob@example.com")).thenReturn(true);
        RegisterRequest request = new RegisterRequest("Bob", "Bob@Example.com", "password123", Role.CUSTOMER, null, null);

        assertThatThrownBy(() -> authService.register(request)).isInstanceOf(ConflictException.class);
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("a vendor must provide a store name")
    void vendorNeedsStoreName() {
        when(userRepository.existsByEmail("v@example.com")).thenReturn(false);
        RegisterRequest request = new RegisterRequest("Vee", "v@example.com", "password123", Role.VENDOR, "  ", null);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("storeName");
    }

    @Test
    @DisplayName("a new customer gets a hashed password, no vendor profile and a token")
    void registerCustomer() {
        when(userRepository.existsByEmail("c@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(5L);
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        AuthResponse response = authService.register(
                new RegisterRequest("Cara", "C@example.com", "password123", Role.CUSTOMER, null, null));

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("HASHED");
        assertThat(captor.getValue().getEmail()).isEqualTo("c@example.com");
        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.role()).isEqualTo(Role.CUSTOMER);
        verify(vendorRepository, never()).save(any(Vendor.class));
    }

    @Test
    @DisplayName("a new vendor starts as PENDING")
    void registerVendorStartsPending() {
        when(userRepository.existsByEmail("v@example.com")).thenReturn(false);
        when(vendorRepository.existsByStoreNameIgnoreCase("Gadget Hub")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("HASHED");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User saved = inv.getArgument(0);
            saved.setId(6L);
            return saved;
        });
        when(jwtService.generateToken(any())).thenReturn("jwt-token");

        authService.register(new RegisterRequest("Vee", "v@example.com", "password123",
                Role.VENDOR, "Gadget Hub", "Phones and more"));

        ArgumentCaptor<Vendor> captor = ArgumentCaptor.forClass(Vendor.class);
        verify(vendorRepository).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(VendorStatus.PENDING);
        assertThat(captor.getValue().getStoreName()).isEqualTo("Gadget Hub");
    }
}
