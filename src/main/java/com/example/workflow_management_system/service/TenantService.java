package com.example.workflow_management_system.service;

import com.example.workflow_management_system.dto.TenantRequest;
import com.example.workflow_management_system.dto.TenantResponse;
import com.example.workflow_management_system.model.Tenant;
import com.example.workflow_management_system.model.TenantStatus;
import com.example.workflow_management_system.repository.TenantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;

    public TenantService(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    public TenantResponse createTenant(TenantRequest request) {
        if (tenantRepository.existsByName(request.name())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Tenant with name '" + request.name() + "' already exists");
        }
        Tenant tenant = new Tenant(request.name(), TenantStatus.ACTIVE);
        Tenant savedTenant = tenantRepository.save(tenant);
        return  mapToResponse(savedTenant);
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> getAllTenants() {
        return tenantRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenantById(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with id: " + id));
        return mapToResponse(tenant);
    }

    public TenantResponse updateTenant(Long id, TenantRequest request) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with id: " + id));

        // Check if name is taken by another tenant
        tenantRepository.findByName(request.name()).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Tenant with name '" + request.name() + "' already exists");
            }
        });

        tenant.setName(request.name());
        Tenant updatedTenant = tenantRepository.save(tenant);
        return mapToResponse(updatedTenant);
    }

    public void deleteTenant(Long id) {
        Tenant tenant = tenantRepository.findById(id)
                .orElseThrow(
                        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Tenant not found with id: " + id));

        tenant.setStatus(TenantStatus.INACTIVE);
        tenantRepository.save(tenant);
    }

    private TenantResponse mapToResponse(Tenant tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedAt());
    }
}
