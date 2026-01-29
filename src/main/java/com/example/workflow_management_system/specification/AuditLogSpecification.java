package com.example.workflow_management_system.specification;

import com.example.workflow_management_system.model.AuditLog;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class AuditLogSpecification {
    public static Specification<AuditLog> filterAuditLogs(Long tenantId, String entityType, String entityId,
            String action, LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant Isolation (Mandatory)
            if (tenantId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            }

            // Optional Filters
            if (entityType != null && !entityType.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("entityType"), entityType));
            }
            if (entityId != null && !entityId.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("entityId"), entityId));
            }
            if (action != null && !action.isBlank()) {
                predicates.add(criteriaBuilder.equal(root.get("action"), action));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("timestamp"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("timestamp"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }
}
