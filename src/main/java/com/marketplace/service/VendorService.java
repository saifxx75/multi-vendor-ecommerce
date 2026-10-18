package com.marketplace.service;

import com.marketplace.dto.VendorResponse;
import com.marketplace.entity.Vendor;
import com.marketplace.entity.VendorStatus;
import com.marketplace.exception.BadRequestException;
import com.marketplace.exception.ResourceNotFoundException;
import com.marketplace.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VendorService {

    private final VendorRepository vendorRepository;

    @Transactional(readOnly = true)
    public List<VendorResponse> list(VendorStatus status) {
        List<Vendor> vendors = status == null ? vendorRepository.findAll() : vendorRepository.findByStatus(status);
        return vendors.stream().map(Mapper::toVendor).toList();
    }

    @Transactional
    public VendorResponse updateStatus(Long vendorId, VendorStatus newStatus) {
        if (newStatus == VendorStatus.PENDING) {
            throw new BadRequestException("A vendor can only be set to APPROVED or REJECTED");
        }
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor not found"));
        vendor.setStatus(newStatus);
        return Mapper.toVendor(vendor);
    }

    @Transactional(readOnly = true)
    public VendorResponse profile(Long userId) {
        return Mapper.toVendor(getVendorForUser(userId));
    }

    @Transactional(readOnly = true)
    public Vendor getVendorForUser(Long userId) {
        return vendorRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Vendor profile not found for this account"));
    }

    /** Selling (creating/editing products) requires admin approval of the store. */
    @Transactional(readOnly = true)
    public Vendor getApprovedVendorForUser(Long userId) {
        Vendor vendor = getVendorForUser(userId);
        if (vendor.getStatus() != VendorStatus.APPROVED) {
            throw new AccessDeniedException(
                    "Your vendor account is " + vendor.getStatus() + ". Only approved vendors can manage products");
        }
        return vendor;
    }
}
