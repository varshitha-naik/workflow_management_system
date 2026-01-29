package com.example.workflow_management_system.specification;

import com.example.workflow_management_system.model.Workflow;
import org.springframework.data.jpa.domain.Specification;

public class WorkflowSpecification {

    public static Specification<Workflow> withTenantId(Long tenantId) {
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(root.get("tenant").get("id"), tenantId);
    }

    public static Specification<Workflow> search(String query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            if (query == null || query.isBlank()) {
                return criteriaBuilder.conjunction();
            }
            String likePattern = "%" + query.toLowerCase() + "%";
            return criteriaBuilder.or(
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), likePattern),
                    criteriaBuilder.like(criteriaBuilder.lower(root.get("description")), likePattern));
        };
    }
}
