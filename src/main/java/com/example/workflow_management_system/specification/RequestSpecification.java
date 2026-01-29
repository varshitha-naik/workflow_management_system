package com.example.workflow_management_system.specification;

import com.example.workflow_management_system.model.Request;
import com.example.workflow_management_system.model.RequestStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import jakarta.persistence.criteria.Predicate;

public class RequestSpecification {
    public static Specification<Request> filterRequests(Long tenantId, RequestStatus status, Long workflowId,
            Long createdByUserId, LocalDateTime fromDate, LocalDateTime toDate) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tenant Isolation (Mandatory)
            if (tenantId != null) {
                predicates.add(criteriaBuilder.equal(root.get("tenantId"), tenantId));
            }

            // Optional Filters
            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }
            if (workflowId != null) {
                predicates.add(criteriaBuilder.equal(root.get("workflow").get("id"), workflowId));
            }
            if (createdByUserId != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy").get("id"), createdByUserId));
            }
            if (fromDate != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
            }
            if (toDate != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), toDate));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    public static Specification<Request> search(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + query.toLowerCase() + "%";

            Specification<Request> spec = (r, q, cb) -> cb.or(
                    cb.like(cb.lower(r.get("payload")), likePattern),
                    cb.like(cb.lower(r.get("workflow").get("name")), likePattern),
                    cb.like(cb.lower(r.get("createdBy").get("username")), likePattern));

            // If query is numeric, also search by ID
            if (query.matches("\\d+")) {
                Specification<Request> idSpec = (r, q, cb) -> cb.equal(r.get("id"), Long.valueOf(query));
                spec = spec.or(idSpec);
            }

            return spec.toPredicate(root, criteriaQuery, criteriaBuilder);
        };
    }
}
