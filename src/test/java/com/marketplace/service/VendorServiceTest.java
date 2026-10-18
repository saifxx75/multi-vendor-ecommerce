package com.marketplace.service;

import com.marketplace.entity.Vendor;
import com.marketplace.entity.VendorStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.VendorRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VendorServiceTest {

    @Mock
    private VendorRepository vendorRepository;

    @InjectMocks
    private VendorService vendorService;

    private Vendor vendorWithStatus(VendorStatus status) {
        Vendor vendor = new Vendor();
        vendor.setId(3L);
        vendor.setStatus(status);
        return vendor;
    }

    @Test
    @DisplayName("an approved vendor may manage products")
    void approvedVendorIsAllowed() {
        Vendor vendor = vendorWithStatus(VendorStatus.APPROVED);
        when(vendorRepository.findByUserId(2L)).thenReturn(Optional.of(vendor));

        assertThat(vendorService.getApprovedVendorForUser(2L)).isSameAs(vendor);
    }

    @Test
    @DisplayName("a pending vendor is denied")
    void pendingVendorIsDenied() {
        when(vendorRepository.findByUserId(2L)).thenReturn(Optional.of(vendorWithStatus(VendorStatus.PENDING)));

        assertThatThrownBy(() -> vendorService.getApprovedVendorForUser(2L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    @DisplayName("a rejected vendor is denied")
    void rejectedVendorIsDenied() {
        when(vendorRepository.findByUserId(2L)).thenReturn(Optional.of(vendorWithStatus(VendorStatus.REJECTED)));

        assertThatThrownBy(() -> vendorService.getApprovedVendorForUser(2L))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("a user without a vendor profile gets not-found")
    void missingVendorProfile() {
        when(vendorRepository.findByUserId(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.getVendorForUser(9L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("an admin cannot move a vendor back to PENDING")
    void cannotSetPending() {
        assertThatThrownBy(() -> vendorService.updateStatus(3L, VendorStatus.PENDING))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    @DisplayName("updating the status of an unknown vendor is not-found")
    void updateUnknownVendor() {
        when(vendorRepository.findById(77L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorService.updateStatus(77L, VendorStatus.APPROVED))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
