package com.example.workflow_management_system.specification;

import com.example.workflow_management_system.model.User;
import org.springframework.data.jpa.domain.Specification;

public class UserSpecification {

    public static Specification<User> withTenantId(Long tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);
    }

    public static Specification<User> search(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + query.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("username")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), likePattern));
        };
    }
}
