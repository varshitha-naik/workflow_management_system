package com.example.workflow_management_system.aspect;

import com.example.workflow_management_system.security.TenantContext;
import jakarta.persistence.EntityManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.hibernate.Session;
import org.hibernate.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantFilterAspect {

    private static final Logger logger = LoggerFactory.getLogger(TenantFilterAspect.class);
    private final EntityManager entityManager;

    public TenantFilterAspect(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    // Intercept all Service methods. We use Service because Transactions often
    // start here.
    // Alternatively, intercept @Transactional methods.
    @Pointcut("within(@org.springframework.stereotype.Service *)")
    public void serviceLayer() {
    }

    @Pointcut("@annotation(org.springframework.transaction.annotation.Transactional)")
    public void transactionalMethod() {
    }

    // Combine: Any method in a Service or any method marked Transactional
    // But filters need an active Session.
    @Around("serviceLayer() || transactionalMethod()")
    public Object enableTenantFilter(ProceedingJoinPoint joinPoint) throws Throwable {
        Long tenantId = TenantContext.getTenantId();
        logger.debug("DEBUG: TenantFilterAspect - Intercepting method. TenantContext ID: {}", tenantId);

        if (tenantId != null) {
            try {
                // Determine if we have a valid session to enable the filter on
                Session session = entityManager.unwrap(Session.class);
                Filter filter = session.enableFilter("tenantFilter");
                filter.setParameter("tenantId", tenantId);
                logger.debug("Tenant filter enabled for tenantId = {}", tenantId);
            } catch (Exception e) {
                // If no session/EntityManager available (e.g. strict DTO logic), ignore.
                // But usually in Service layer with JPA, we have one.
                // logger.warn("Could not enable tenant filter: {}", e.getMessage());
            }
        }

        return joinPoint.proceed();
    }
}
